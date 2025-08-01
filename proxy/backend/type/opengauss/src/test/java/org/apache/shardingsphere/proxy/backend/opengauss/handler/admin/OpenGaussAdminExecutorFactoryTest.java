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

package org.apache.shardingsphere.proxy.backend.opengauss.handler.admin;

import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.context.statement.type.CommonSQLStatementContext;
import org.apache.shardingsphere.infra.binder.context.statement.type.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.database.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.metadata.statistics.collector.DialectDatabaseStatisticsCollector;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutor;
import org.apache.shardingsphere.proxy.backend.postgresql.handler.admin.PostgreSQLAdminExecutorCreator;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.ShowStatement;
import org.apache.shardingsphere.test.mock.AutoMockExtension;
import org.apache.shardingsphere.test.mock.StaticMockSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.configuration.plugins.Plugins;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AutoMockExtension.class)
@StaticMockSettings(DatabaseTypedSPILoader.class)
class OpenGaussAdminExecutorFactoryTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "openGauss");
    
    @Mock
    private PostgreSQLAdminExecutorCreator postgresqlAdminExecutorFactory;
    
    private OpenGaussAdminExecutorCreator openGaussAdminExecutorFactory;
    
    @BeforeEach
    void setup() throws ReflectiveOperationException {
        openGaussAdminExecutorFactory = new OpenGaussAdminExecutorCreator();
        Plugins.getMemberAccessor().set(OpenGaussAdminExecutorCreator.class.getDeclaredField("delegated"), openGaussAdminExecutorFactory, postgresqlAdminExecutorFactory);
    }
    
    @Test
    void assertNewInstanceWithSQLStatementContext() {
        SQLStatementContext sqlStatementContext = new CommonSQLStatementContext(new ShowStatement(databaseType, "all"));
        Optional<DatabaseAdminExecutor> actual = openGaussAdminExecutorFactory.create(sqlStatementContext);
        assertTrue(actual.isPresent());
    }
    
    @Test
    void assertNewInstanceWithOtherSQL() {
        DialectDatabaseStatisticsCollector statisticsCollector = mock(DialectDatabaseStatisticsCollector.class);
        when(statisticsCollector.isStatisticsTables(anyMap())).thenReturn(false);
        when(DatabaseTypedSPILoader.findService(DialectDatabaseStatisticsCollector.class, TypedSPILoader.getService(DatabaseType.class, "openGauss"))).thenReturn(Optional.of(statisticsCollector));
        SelectStatementContext sqlStatementContext = mock(SelectStatementContext.class, RETURNS_DEEP_STUBS);
        when(sqlStatementContext.getTablesContext().getTableNames()).thenReturn(Collections.emptyList());
        DatabaseAdminExecutor expected = mock(DatabaseAdminExecutor.class);
        when(postgresqlAdminExecutorFactory.create(sqlStatementContext, "", "", Collections.emptyList())).thenReturn(Optional.of(expected));
        Optional<DatabaseAdminExecutor> actual = openGaussAdminExecutorFactory.create(sqlStatementContext, "", "", Collections.emptyList());
        assertTrue(actual.isPresent());
        assertThat(actual.get(), is(expected));
    }
}
