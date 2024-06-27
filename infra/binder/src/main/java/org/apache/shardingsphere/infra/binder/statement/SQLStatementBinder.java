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

package org.apache.shardingsphere.infra.binder.statement;

import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;

/**
 * SQL statement binder.
 * 
 * @param <T> type of SQL statement
 */
public interface SQLStatementBinder<T extends SQLStatement> {
    
    /**
     * Bind SQL statement with metadata.
     *
     * @param sqlStatement sql statement
     * @param metaData meta data
     * @param currentDatabaseName current database name
     * @return bounded sql statement
     */
    T bind(T sqlStatement, ShardingSphereMetaData metaData, String currentDatabaseName);
}
