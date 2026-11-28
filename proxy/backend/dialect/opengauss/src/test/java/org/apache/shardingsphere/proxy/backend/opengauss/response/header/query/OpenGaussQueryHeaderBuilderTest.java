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

package org.apache.shardingsphere.proxy.backend.opengauss.response.header.query;

import org.apache.shardingsphere.database.protocol.opengauss.type.OpenGaussColumnTypeOIDResolver;
import org.apache.shardingsphere.driver.jdbc.core.resultset.ShardingSphereResultSetMetaData;
import org.apache.shardingsphere.proxy.backend.postgresql.response.header.query.PostgreSQLQueryHeaderBuilder;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeader;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenGaussQueryHeaderBuilderTest {
    
    @Test
    void assertBuildOpenGaussQueryHeader() throws SQLException {
        int columnIndex = 1;
        ShardingSphereResultSetMetaData resultSetMetaData = mock(ShardingSphereResultSetMetaData.class);
        when(resultSetMetaData.getColumnLabel(columnIndex)).thenReturn("label");
        when(resultSetMetaData.getColumnType(columnIndex)).thenReturn(Types.INTEGER);
        when(resultSetMetaData.getColumnTypeName(columnIndex)).thenReturn("int");
        when(resultSetMetaData.getColumnDisplaySize(columnIndex)).thenReturn(11);
        QueryHeader expected = new PostgreSQLQueryHeaderBuilder().build(resultSetMetaData, null, null, resultSetMetaData.getColumnLabel(columnIndex), columnIndex);
        QueryHeader actual = new OpenGaussQueryHeaderBuilder().build(resultSetMetaData, null, null, resultSetMetaData.getColumnLabel(columnIndex), columnIndex);
        assertThat(actual.getColumnLabel(), is(expected.getColumnLabel()));
        assertThat(actual.getColumnType(), is(expected.getColumnType()));
        assertThat(actual.getColumnTypeName(), is(expected.getColumnTypeName()));
        assertThat(actual.getColumnLength(), is(expected.getColumnLength()));
    }
    
    @Test
    void assertAppendProtocolAttributes() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        Statement statement = mock(Statement.class);
        Connection connection = mock(Connection.class);
        when(resultSet.getStatement()).thenReturn(statement);
        when(statement.getConnection()).thenReturn(connection);
        // AutoMockExtension cannot configure constructed mocks, while this scenario requires a non-empty resolver result.
        try (MockedConstruction<OpenGaussColumnTypeOIDResolver> mockedConstruction = mockConstruction(OpenGaussColumnTypeOIDResolver.class, (mock, context) -> {
            when(mock.findTypeOID(connection, "record_type")).thenReturn(Optional.of(2249));
            when(mock.findTypeOID(connection, "foo_record_type")).thenReturn(Optional.of(2250));
        })) {
            OpenGaussQueryHeaderBuilder builder = new OpenGaussQueryHeaderBuilder();
            QueryHeader firstHeader = new QueryHeader("", "", "foo_first", "", Types.STRUCT, "record_type", 0, 0, false, false, false, false);
            QueryHeader ordinaryHeader = new QueryHeader("", "", "foo_id", "", Types.INTEGER, "int", 0, 0, false, false, false, false);
            QueryHeader secondHeader = new QueryHeader("", "", "foo_second", "", Types.STRUCT, "foo_record_type", 0, 0, false, false, false, false);
            builder.appendProtocolAttributes(Arrays.asList(firstHeader, ordinaryHeader, secondHeader), resultSet);
            assertThat(firstHeader.getProtocolAttributes().get(PostgreSQLQueryHeaderBuilder.TYPE_OID), is(2249));
            assertTrue(ordinaryHeader.getProtocolAttributes().isEmpty());
            assertThat(secondHeader.getProtocolAttributes().get(PostgreSQLQueryHeaderBuilder.TYPE_OID), is(2250));
            verify(mockedConstruction.constructed().get(0), never()).findTypeOID(connection, "int");
        }
    }
}
