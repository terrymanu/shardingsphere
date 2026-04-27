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

package org.apache.shardingsphere.mcp.bootstrap.fixture;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Metadata-only JDBC driver for bootstrap STDIO tests.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BootstrapMockRuntimeDriver implements Driver {
    
    private static final BootstrapMockRuntimeDriver INSTANCE = new BootstrapMockRuntimeDriver();
    
    private static final String JDBC_URL_PREFIX = "jdbc:bootstrap-mock:";
    
    private static final String DATABASE_PRODUCT_NAME = "MySQL";
    
    private static final String DATABASE_PRODUCT_VERSION = "8.0.36";
    
    private static final List<String> TABLE_NAMES = List.of("order_items", "orders");
    
    private static final Map<String, List<String>> TABLE_COLUMNS = Map.of(
            "orders", List.of("amount", "order_id", "status"),
            "order_items", List.of("item_id", "order_id", "sku"));
    
    private static final Map<String, List<String>> TABLE_INDEXES = Map.of(
            "orders", List.of("PRIMARY_KEY_C", "idx_orders_status"),
            "order_items", List.of("PRIMARY_KEY_C"));
    
    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (final SQLException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
    
    /**
     * Create one JDBC URL for the bootstrap mock runtime.
     *
     * @param databaseName database name
     * @return JDBC URL
     */
    public static String createJdbcUrl(final String databaseName) {
        return JDBC_URL_PREFIX + databaseName;
    }
    
    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        return acceptsURL(url) ? createConnection(url) : null;
    }
    
    @Override
    public boolean acceptsURL(final String url) {
        return null != url && url.startsWith(JDBC_URL_PREFIX);
    }
    
    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) {
        return new DriverPropertyInfo[0];
    }
    
    @Override
    public int getMajorVersion() {
        return 1;
    }
    
    @Override
    public int getMinorVersion() {
        return 0;
    }
    
    @Override
    public boolean jdbcCompliant() {
        return false;
    }
    
    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }
    
    private static Connection createConnection(final String jdbcUrl) {
        ConnectionState state = new ConnectionState(jdbcUrl);
        return (Connection) Proxy.newProxyInstance(BootstrapMockRuntimeDriver.class.getClassLoader(),
                new Class[]{Connection.class}, (proxy, method, args) -> handleConnectionMethod(state, proxy, method, args));
    }
    
    private static Object handleConnectionMethod(final ConnectionState state, final Object proxy, final Method method, final Object[] args) {
        String methodName = method.getName();
        if ("getMetaData".equals(methodName)) {
            return createDatabaseMetaData(state.jdbcUrl);
        }
        if ("close".equals(methodName)) {
            state.closed = true;
            return null;
        }
        if ("isClosed".equals(methodName)) {
            return state.closed;
        }
        if ("isValid".equals(methodName)) {
            return !state.closed;
        }
        return handleCommonObjectMethod(proxy, method, args);
    }
    
    private static DatabaseMetaData createDatabaseMetaData(final String jdbcUrl) {
        return (DatabaseMetaData) Proxy.newProxyInstance(BootstrapMockRuntimeDriver.class.getClassLoader(),
                new Class[]{DatabaseMetaData.class}, (proxy, method, args) -> handleDatabaseMetaDataMethod(jdbcUrl, proxy, method, args));
    }
    
    private static Object handleDatabaseMetaDataMethod(final String jdbcUrl, final Object proxy, final Method method, final Object[] args) {
        String methodName = method.getName();
        if ("getDatabaseProductName".equals(methodName)) {
            return DATABASE_PRODUCT_NAME;
        }
        if ("getDatabaseProductVersion".equals(methodName)) {
            return DATABASE_PRODUCT_VERSION;
        }
        if ("getURL".equals(methodName)) {
            return jdbcUrl;
        }
        if ("getTables".equals(methodName)) {
            return createTablesResultSet((String[]) args[3]);
        }
        if ("getColumns".equals(methodName)) {
            return createColumnsResultSet(String.valueOf(args[2]));
        }
        if ("getIndexInfo".equals(methodName)) {
            return createIndexesResultSet(String.valueOf(args[2]));
        }
        return handleCommonObjectMethod(proxy, method, args);
    }
    
    private static ResultSet createTablesResultSet(final String[] types) {
        if (null != types && !List.of(types).contains("TABLE")) {
            return createResultSet(List.of());
        }
        return createResultSet(TABLE_NAMES.stream().map(each -> Map.of("TABLE_NAME", each)).toList());
    }
    
    private static ResultSet createColumnsResultSet(final String objectName) {
        return createResultSet(TABLE_COLUMNS.getOrDefault(objectName, List.of()).stream().map(each -> Map.of("COLUMN_NAME", each)).toList());
    }
    
    private static ResultSet createIndexesResultSet(final String tableName) {
        return createResultSet(TABLE_INDEXES.getOrDefault(tableName, List.of()).stream().map(each -> Map.of("INDEX_NAME", each)).toList());
    }
    
    private static ResultSet createResultSet(final List<Map<String, String>> rows) {
        ResultSetState state = new ResultSetState(rows);
        return (ResultSet) Proxy.newProxyInstance(BootstrapMockRuntimeDriver.class.getClassLoader(),
                new Class[]{ResultSet.class}, (proxy, method, args) -> handleResultSetMethod(state, proxy, method, args));
    }
    
    private static Object handleResultSetMethod(final ResultSetState state, final Object proxy, final Method method, final Object[] args) {
        String methodName = method.getName();
        if ("next".equals(methodName)) {
            if (state.rowIndex + 1 < state.rows.size()) {
                state.rowIndex++;
                return true;
            }
            state.rowIndex = state.rows.size();
            return false;
        }
        if ("getString".equals(methodName)) {
            return state.getValue(String.valueOf(args[0]));
        }
        if ("close".equals(methodName)) {
            state.closed = true;
            return null;
        }
        if ("isClosed".equals(methodName)) {
            return state.closed;
        }
        if ("wasNull".equals(methodName)) {
            return false;
        }
        return handleCommonObjectMethod(proxy, method, args);
    }
    
    private static Object handleCommonObjectMethod(final Object proxy, final Method method, final Object[] args) {
        String methodName = method.getName();
        if ("toString".equals(methodName)) {
            return proxy.getClass().getName();
        }
        if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(methodName)) {
            return proxy == args[0];
        }
        if ("unwrap".equals(methodName)) {
            return null;
        }
        if ("isWrapperFor".equals(methodName)) {
            return false;
        }
        return getDefaultValue(method.getReturnType());
    }
    
    private static Object getDefaultValue(final Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class == returnType) {
            return false;
        }
        if (int.class == returnType || short.class == returnType || byte.class == returnType) {
            return 0;
        }
        if (long.class == returnType) {
            return 0L;
        }
        if (float.class == returnType) {
            return 0F;
        }
        if (double.class == returnType) {
            return 0D;
        }
        if (char.class == returnType) {
            return '\0';
        }
        return null;
    }
    
    private static final class ConnectionState {
        
        private final String jdbcUrl;
        
        private boolean closed;
        
        private ConnectionState(final String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }
    }
    
    private static final class ResultSetState {
        
        private final List<Map<String, String>> rows;
        
        private int rowIndex = -1;
        
        private boolean closed;
        
        private ResultSetState(final List<Map<String, String>> rows) {
            this.rows = rows;
        }
        
        private String getValue(final String columnLabel) {
            return rows.get(rowIndex).get(columnLabel);
        }
    }
}
