/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.scenario.migration;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.data.pipeline.api.PipelineDataSourceConfiguration;
import org.apache.shardingsphere.data.pipeline.api.type.ShardingSpherePipelineDataSourceConfiguration;
import org.apache.shardingsphere.data.pipeline.core.context.TransmissionProcessContext;
import org.apache.shardingsphere.data.pipeline.core.datanode.JobDataNodeEntry;
import org.apache.shardingsphere.data.pipeline.core.datasource.DefaultPipelineDataSourceManager;
import org.apache.shardingsphere.data.pipeline.core.datasource.PipelineDataSourceManager;
import org.apache.shardingsphere.data.pipeline.core.importer.ImporterConfiguration;
import org.apache.shardingsphere.data.pipeline.core.ingest.dumper.context.IncrementalDumperContext;
import org.apache.shardingsphere.data.pipeline.core.ingest.dumper.context.mapper.TableAndSchemaNameMapper;
import org.apache.shardingsphere.data.pipeline.core.job.AbstractSeparablePipelineJob;
import org.apache.shardingsphere.data.pipeline.core.job.id.PipelineJobIdUtils;
import org.apache.shardingsphere.data.pipeline.core.job.progress.TransmissionJobItemProgress;
import org.apache.shardingsphere.data.pipeline.core.job.progress.config.PipelineProcessConfiguration;
import org.apache.shardingsphere.data.pipeline.core.job.progress.config.PipelineProcessConfigurationUtils;
import org.apache.shardingsphere.data.pipeline.core.job.service.PipelineJobItemManager;
import org.apache.shardingsphere.data.pipeline.core.metadata.CaseInsensitiveIdentifier;
import org.apache.shardingsphere.data.pipeline.core.metadata.CaseInsensitiveQualifiedTable;
import org.apache.shardingsphere.data.pipeline.core.metadata.PipelineProcessConfigurationPersistService;
import org.apache.shardingsphere.data.pipeline.core.preparer.CreateTableConfiguration;
import org.apache.shardingsphere.data.pipeline.core.spi.algorithm.JobRateLimitAlgorithm;
import org.apache.shardingsphere.data.pipeline.core.task.runner.PipelineTasksRunner;
import org.apache.shardingsphere.data.pipeline.core.task.runner.TransmissionTasksRunner;
import org.apache.shardingsphere.data.pipeline.core.util.ShardingColumnsExtractor;
import org.apache.shardingsphere.data.pipeline.scenario.migration.config.MigrationJobConfiguration;
import org.apache.shardingsphere.data.pipeline.scenario.migration.config.MigrationTaskConfiguration;
import org.apache.shardingsphere.data.pipeline.scenario.migration.config.yaml.swapper.YamlMigrationJobConfigurationSwapper;
import org.apache.shardingsphere.data.pipeline.scenario.migration.context.MigrationJobItemContext;
import org.apache.shardingsphere.data.pipeline.scenario.migration.ingest.dumper.MigrationIncrementalDumperContextCreator;
import org.apache.shardingsphere.data.pipeline.scenario.migration.preparer.MigrationJobPreparer;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;
import org.apache.shardingsphere.infra.database.core.type.DatabaseTypeRegistry;
import org.apache.shardingsphere.infra.datanode.DataNode;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Migration job.
 */
@Slf4j
public final class MigrationJob extends AbstractSeparablePipelineJob<MigrationJobItemContext> {
    
    private final PipelineJobItemManager<TransmissionJobItemProgress> jobItemManager;
    
    private final PipelineProcessConfigurationPersistService processConfigPersistService;
    
    private final PipelineDataSourceManager dataSourceManager;
    
    // Shared by all sharding items
    private final MigrationJobPreparer jobPreparer;
    
    public MigrationJob(final String jobId) {
        super(jobId);
        jobItemManager = new PipelineJobItemManager<>(new MigrationJobType().getYamlJobItemProgressSwapper());
        processConfigPersistService = new PipelineProcessConfigurationPersistService();
        dataSourceManager = new DefaultPipelineDataSourceManager();
        jobPreparer = new MigrationJobPreparer();
    }
    
    @Override
    protected MigrationJobItemContext buildJobItemContext(final ShardingContext shardingContext) {
        MigrationJobConfiguration jobConfig = new YamlMigrationJobConfigurationSwapper().swapToObject(shardingContext.getJobParameter());
        int shardingItem = shardingContext.getShardingItem();
        Optional<TransmissionJobItemProgress> initProgress = jobItemManager.getProgress(shardingContext.getJobName(), shardingItem);
        PipelineProcessConfiguration processConfig = PipelineProcessConfigurationUtils.convertWithDefaultValue(
                processConfigPersistService.load(PipelineJobIdUtils.parseContextKey(jobConfig.getJobId()), "MIGRATION"));
        TransmissionProcessContext jobProcessContext = new TransmissionProcessContext(jobConfig.getJobId(), processConfig);
        MigrationTaskConfiguration taskConfig = buildTaskConfiguration(jobConfig, shardingItem, jobProcessContext.getPipelineProcessConfig());
        return new MigrationJobItemContext(jobConfig, shardingItem, initProgress.orElse(null), jobProcessContext, taskConfig, dataSourceManager);
    }
    
    private MigrationTaskConfiguration buildTaskConfiguration(final MigrationJobConfiguration jobConfig, final int jobShardingItem, final PipelineProcessConfiguration processConfig) {
        IncrementalDumperContext incrementalDumperContext = new MigrationIncrementalDumperContextCreator(jobConfig).createDumperContext(jobConfig.getJobShardingDataNodes().get(jobShardingItem));
        Collection<CreateTableConfiguration> createTableConfigs = buildCreateTableConfigurations(jobConfig, incrementalDumperContext.getCommonContext().getTableAndSchemaNameMapper());
        Set<CaseInsensitiveIdentifier> targetTableNames = jobConfig.getTargetTableNames().stream().map(CaseInsensitiveIdentifier::new).collect(Collectors.toSet());
        Map<CaseInsensitiveIdentifier, Set<String>> shardingColumnsMap = new ShardingColumnsExtractor().getShardingColumnsMap(
                ((ShardingSpherePipelineDataSourceConfiguration) jobConfig.getTarget()).getRootConfig().getRules(), targetTableNames);
        ImporterConfiguration importerConfig = buildImporterConfiguration(jobConfig, processConfig, shardingColumnsMap, incrementalDumperContext.getCommonContext().getTableAndSchemaNameMapper());
        return new MigrationTaskConfiguration(incrementalDumperContext.getCommonContext().getDataSourceName(), createTableConfigs, incrementalDumperContext, importerConfig);
    }
    
    private Collection<CreateTableConfiguration> buildCreateTableConfigurations(final MigrationJobConfiguration jobConfig, final TableAndSchemaNameMapper mapper) {
        Collection<CreateTableConfiguration> result = new LinkedList<>();
        for (JobDataNodeEntry each : jobConfig.getTablesFirstDataNodes().getEntries()) {
            String sourceSchemaName = mapper.getSchemaName(each.getLogicTableName());
            String targetSchemaName = new DatabaseTypeRegistry(jobConfig.getTargetDatabaseType()).getDialectDatabaseMetaData().isSchemaAvailable() ? sourceSchemaName : null;
            DataNode dataNode = each.getDataNodes().get(0);
            PipelineDataSourceConfiguration sourceDataSourceConfig = jobConfig.getSources().get(dataNode.getDataSourceName());
            CreateTableConfiguration createTableConfig = new CreateTableConfiguration(sourceDataSourceConfig, new CaseInsensitiveQualifiedTable(sourceSchemaName, dataNode.getTableName()),
                    jobConfig.getTarget(), new CaseInsensitiveQualifiedTable(targetSchemaName, each.getLogicTableName()));
            result.add(createTableConfig);
        }
        return result;
    }
    
    private ImporterConfiguration buildImporterConfiguration(final MigrationJobConfiguration jobConfig, final PipelineProcessConfiguration pipelineProcessConfig,
                                                             final Map<CaseInsensitiveIdentifier, Set<String>> shardingColumnsMap, final TableAndSchemaNameMapper mapper) {
        int batchSize = pipelineProcessConfig.getWrite().getBatchSize();
        JobRateLimitAlgorithm writeRateLimitAlgorithm = new TransmissionProcessContext(jobConfig.getJobId(), pipelineProcessConfig).getWriteRateLimitAlgorithm();
        int retryTimes = jobConfig.getRetryTimes();
        int concurrency = jobConfig.getConcurrency();
        return new ImporterConfiguration(jobConfig.getTarget(), shardingColumnsMap, mapper, batchSize, writeRateLimitAlgorithm, retryTimes, concurrency);
    }
    
    @Override
    protected PipelineTasksRunner buildTasksRunner(final MigrationJobItemContext jobItemContext) {
        return new TransmissionTasksRunner(jobItemContext);
    }
    
    @Override
    protected void doPrepare(final MigrationJobItemContext jobItemContext) throws SQLException {
        jobPreparer.prepare(jobItemContext);
    }
    
    @Override
    public void clean() {
        dataSourceManager.close();
    }
}
