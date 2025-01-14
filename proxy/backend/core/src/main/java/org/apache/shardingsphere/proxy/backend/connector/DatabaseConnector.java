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

package org.apache.shardingsphere.proxy.backend.connector;

import org.apache.shardingsphere.proxy.backend.handler.data.DatabaseBackendHandler;
import org.apache.shardingsphere.proxy.backend.response.data.QueryResponseRow;
import org.apache.shardingsphere.proxy.backend.response.header.ResponseHeader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database connector.
 */
public interface DatabaseConnector extends DatabaseBackendHandler {
    
    /**
     * Add statement.
     *
     * @param statement statement to be added
     */
    void add(Statement statement);
    
    /**
     * Add result set.
     *
     * @param resultSet result set to be added
     */
    void add(ResultSet resultSet);
    
    @Override
    ResponseHeader execute() throws SQLException;
    
    @Override
    boolean next() throws SQLException;
    
    @Override
    QueryResponseRow getRowData() throws SQLException;
    
    @Override
    void close() throws SQLException;
}
