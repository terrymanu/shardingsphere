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

package org.apache.shardingsphere.distsql.handler.engine.update.rdl.rule.engine.database.type;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.distsql.handler.engine.update.rdl.rule.engine.database.DatabaseRuleOperator;
import org.apache.shardingsphere.distsql.handler.engine.update.rdl.rule.spi.database.type.DatabaseRuleAlterExecutor;
import org.apache.shardingsphere.distsql.statement.type.rdl.rule.database.DatabaseRuleDefinitionStatement;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.infra.config.rule.checker.DatabaseRuleConfigurationEmptyChecker;
import org.apache.shardingsphere.infra.config.rule.decorator.RuleConfigurationDecorator;
import org.apache.shardingsphere.infra.config.rule.scope.DatabaseRuleConfiguration;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.mode.persist.service.MetaDataManagerPersistService;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Alter database rule operator.
 */
@RequiredArgsConstructor
public final class AlterDatabaseRuleOperator implements DatabaseRuleOperator {
    
    private final ContextManager contextManager;
    
    @SuppressWarnings("rawtypes")
    private final DatabaseRuleAlterExecutor executor;
    
    @Override
    @SuppressWarnings("unchecked")
    public void operate(final DatabaseRuleDefinitionStatement sqlStatement, final ShardingSphereDatabase database, final RuleConfiguration currentRuleConfig) {
        RuleConfiguration toBeAlteredRuleConfig = executor.buildToBeAlteredRuleConfiguration(sqlStatement);
        MetaDataManagerPersistService metaDataManagerPersistService = contextManager.getPersistServiceFacade().getModeFacade().getMetaDataManagerService();
        metaDataManagerPersistService.alterRuleConfiguration(database, decorateRuleConfiguration(database, toBeAlteredRuleConfig));
        RuleConfiguration toBeDroppedRuleConfig = executor.buildToBeDroppedRuleConfiguration(toBeAlteredRuleConfig);
        if (toBeDroppedRuleConfig != null
                && !TypedSPILoader.getService(DatabaseRuleConfigurationEmptyChecker.class, toBeAlteredRuleConfig.getClass()).isEmpty((DatabaseRuleConfiguration) toBeDroppedRuleConfig)) {
            metaDataManagerPersistService.removeRuleConfigurationItem(database, toBeDroppedRuleConfig);
        }
    }
    
    @SuppressWarnings("unchecked")
    private RuleConfiguration decorateRuleConfiguration(final ShardingSphereDatabase database, final RuleConfiguration ruleConfig) {
        return TypedSPILoader.findService(RuleConfigurationDecorator.class, ruleConfig.getClass()).map(optional -> optional.decorate(database.getName(),
                database.getResourceMetaData().getStorageUnits().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getDataSource(), (oldValue, currentValue) -> oldValue, LinkedHashMap::new)),
                database.getRuleMetaData().getRules(), ruleConfig)).orElse(ruleConfig);
    }
}
