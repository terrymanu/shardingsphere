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

package org.apache.shardingsphere.proxy.backend.handler.distsql.ral.updatable.refresh;

import org.apache.shardingsphere.distsql.handler.engine.update.DistSQLUpdateExecutor;
import org.apache.shardingsphere.distsql.statement.type.ral.updatable.RefreshDatabaseMetaDataStatement;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.schema.util.SystemSchemaUtils;
import org.apache.shardingsphere.mode.manager.ContextManager;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

/**
 * Refresh database meta data executor.
 */
public final class RefreshDatabaseMetaDataExecutor implements DistSQLUpdateExecutor<RefreshDatabaseMetaDataStatement> {
    
    @Override
    public void executeUpdate(final RefreshDatabaseMetaDataStatement sqlStatement, final ContextManager contextManager) throws SQLException {
        Collection<ShardingSphereDatabase> databases = sqlStatement.getDatabaseName().map(optional -> (Collection<ShardingSphereDatabase>) Collections.singleton(contextManager.getDatabase(optional)))
                .orElseGet(() -> contextManager.getMetaDataContexts().getMetaData().getAllDatabases());
        for (ShardingSphereDatabase each : databases) {
            if (!SystemSchemaUtils.isSystemSchema(each)) {
                contextManager.reloadDatabase(each);
            }
        }
    }
    
    @Override
    public Class<RefreshDatabaseMetaDataStatement> getType() {
        return RefreshDatabaseMetaDataStatement.class;
    }
}
