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

package org.apache.shardingsphere.proxy.backend.postgresql.response.header.query;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.driver.jdbc.core.resultset.ShardingSphereResultSetMetaData;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeaderBuilder;
import org.junit.jupiter.api.Test;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.TypeInfo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostgreSQLQueryHeaderBuilderTest {
    
    private final QueryHeaderBuilder queryHeaderBuilder = DatabaseTypedSPILoader.getService(QueryHeaderBuilder.class, TypedSPILoader.getService(DatabaseType.class, "PostgreSQL"));
    
    @Test
    void assertBuildPostgreSQLQueryHeader() throws SQLException {
        int columnIndex = 1;
        ShardingSphereResultSetMetaData resultSetMetaData = mock(ShardingSphereResultSetMetaData.class);
        when(resultSetMetaData.getColumnLabel(columnIndex)).thenReturn("label");
        when(resultSetMetaData.getColumnType(columnIndex)).thenReturn(Types.INTEGER);
        when(resultSetMetaData.getColumnTypeName(columnIndex)).thenReturn("int");
        when(resultSetMetaData.getColumnDisplaySize(columnIndex)).thenReturn(11);
        QueryHeader actual = queryHeaderBuilder.build(resultSetMetaData, null, null, resultSetMetaData.getColumnLabel(columnIndex), columnIndex);
        assertThat(actual.getColumnLabel(), is("label"));
        assertThat(actual.getColumnType(), is(Types.INTEGER));
        assertThat(actual.getColumnTypeName(), is("int"));
        assertThat(actual.getColumnLength(), is(11));
    }
    
    @Test
    void assertAppendProtocolAttributes() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        Statement statement = mock(Statement.class);
        BaseConnection connection = mock(BaseConnection.class);
        TypeInfo typeInfo = mock(TypeInfo.class);
        when(resultSet.getStatement()).thenReturn(statement);
        when(statement.getConnection()).thenReturn(connection);
        when(connection.isWrapperFor(BaseConnection.class)).thenReturn(true);
        when(connection.unwrap(BaseConnection.class)).thenReturn(connection);
        when(connection.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.getPGType("record_type")).thenReturn(2249);
        when(typeInfo.getPGType("foo_record_type")).thenReturn(2250);
        QueryHeader firstHeader = new QueryHeader("", "", "foo_first", "", Types.STRUCT, "record_type", 0, 0, false, false, false, false);
        QueryHeader ordinaryHeader = new QueryHeader("", "", "foo_id", "", Types.INTEGER, "int", 0, 0, false, false, false, false);
        QueryHeader secondHeader = new QueryHeader("", "", "foo_second", "", Types.STRUCT, "foo_record_type", 0, 0, false, false, false, false);
        queryHeaderBuilder.appendProtocolAttributes(Arrays.asList(firstHeader, ordinaryHeader, secondHeader), resultSet);
        assertThat(firstHeader.getProtocolAttributes().get(PostgreSQLQueryHeaderBuilder.TYPE_OID), is(2249));
        assertTrue(ordinaryHeader.getProtocolAttributes().isEmpty());
        assertThat(secondHeader.getProtocolAttributes().get(PostgreSQLQueryHeaderBuilder.TYPE_OID), is(2250));
        verify(typeInfo, never()).getPGType("int");
    }
    
    @Test
    void assertAppendProtocolAttributesForNonCompositeColumn() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        QueryHeader queryHeader = new QueryHeader("", "", "foo_id", "", Types.INTEGER, "int", 0, 0, false, false, false, false);
        queryHeaderBuilder.appendProtocolAttributes(Collections.singleton(queryHeader), resultSet);
        assertTrue(queryHeader.getProtocolAttributes().isEmpty());
        verifyNoInteractions(resultSet);
    }
}
