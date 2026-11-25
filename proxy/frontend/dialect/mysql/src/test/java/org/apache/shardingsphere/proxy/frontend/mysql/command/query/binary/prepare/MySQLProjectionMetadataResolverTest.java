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

package org.apache.shardingsphere.proxy.frontend.mysql.command.query.binary.prepare;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.database.protocol.mysql.constant.MySQLBinaryColumnType;
import org.apache.shardingsphere.database.protocol.mysql.constant.MySQLCharacterSets;
import org.apache.shardingsphere.database.protocol.mysql.packet.MySQLPacket;
import org.apache.shardingsphere.database.protocol.mysql.packet.command.query.MySQLColumnDefinition41Packet;
import org.apache.shardingsphere.database.protocol.mysql.packet.command.query.MySQLColumnDefinitionFlag;
import org.apache.shardingsphere.database.protocol.mysql.payload.MySQLPacketPayload;
import org.apache.shardingsphere.infra.binder.context.segment.select.projection.Projection;
import org.apache.shardingsphere.infra.binder.context.statement.type.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.rule.RuleMetaData;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.proxy.frontend.mysql.command.query.binary.MySQLServerPreparedStatement;
import org.apache.shardingsphere.test.infra.framework.extension.mock.AutoMockExtension;
import org.apache.shardingsphere.test.infra.framework.extension.mock.StaticMockSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AutoMockExtension.class)
@StaticMockSettings({ProxyContext.class, MySQLPreparedStatementMetadataFactory.class})
class MySQLProjectionMetadataResolverTest {
    
    @ParameterizedTest(name = "{0}: {1}, nativeYear={2}")
    @MethodSource("columnTypes")
    void assertResolveProjectionPackets(final int jdbcType, final String columnTypeName, final boolean useNativeYearType,
                                        final MySQLBinaryColumnType expectedColumnType, final int expectedFlags) throws SQLException {
        SelectStatementContext selectStatementContext = mock(SelectStatementContext.class, RETURNS_DEEP_STUBS);
        when(selectStatementContext.getTablesContext().getDatabaseName()).thenReturn(Optional.of("foo_db"));
        when(selectStatementContext.getProjectionsContext().getExpandProjections()).thenReturn(Collections.singletonList(mock(Projection.class)));
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
        when(preparedStatement.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        mockResultSetMetaData(resultSetMetaData, 1, jdbcType, columnTypeName);
        ConnectionSession connectionSession = mock(ConnectionSession.class);
        MySQLServerPreparedStatement serverPreparedStatement = mock(MySQLServerPreparedStatement.class);
        when(MySQLPreparedStatementMetadataFactory.load(connectionSession, serverPreparedStatement)).thenReturn(preparedStatement);
        mockDatabase();
        int expectedCharacterSet = MySQLCharacterSets.UTF8MB4_UNICODE_CI.getId();
        Collection<MySQLPacket> actual = MySQLProjectionMetadataResolver.resolveProjectionPackets(connectionSession, serverPreparedStatement,
                selectStatementContext, expectedCharacterSet, useNativeYearType, Collections.emptyList());
        assertThat(actual.size(), is(1));
        MySQLPacketPayload payload = createPayload((MySQLColumnDefinition41Packet) actual.iterator().next());
        try {
            skipColumnDefinitionStrings(payload);
            payload.readIntLenenc();
            assertThat(payload.readInt2(), is(expectedCharacterSet));
            payload.skipReserved(4);
            assertThat(payload.readInt1(), is(expectedColumnType.getValue()));
            assertThat(payload.readInt2(), is(expectedFlags));
        } finally {
            payload.getByteBuf().release();
        }
        verify(preparedStatement).close();
    }
    
    private void mockResultSetMetaData(final ResultSetMetaData resultSetMetaData, final int columnIndex, final int jdbcType, final String columnTypeName) throws SQLException {
        when(resultSetMetaData.getColumnName(columnIndex)).thenReturn("foo_column");
        when(resultSetMetaData.getColumnLabel(columnIndex)).thenReturn("foo_column");
        when(resultSetMetaData.getColumnType(columnIndex)).thenReturn(jdbcType);
        when(resultSetMetaData.getColumnTypeName(columnIndex)).thenReturn(columnTypeName);
        when(resultSetMetaData.getColumnDisplaySize(columnIndex)).thenReturn(1024);
        when(resultSetMetaData.isNullable(columnIndex)).thenReturn(ResultSetMetaData.columnNullable);
        when(resultSetMetaData.getTableName(columnIndex)).thenReturn("");
    }
    
    private void mockDatabase() {
        ContextManager contextManager = mock(ContextManager.class, RETURNS_DEEP_STUBS);
        ShardingSphereDatabase database = mock(ShardingSphereDatabase.class);
        when(database.getName()).thenReturn("foo_db");
        when(database.getRuleMetaData()).thenReturn(new RuleMetaData(Collections.emptyList()));
        when(database.getProtocolType()).thenReturn(TypedSPILoader.getService(DatabaseType.class, "MySQL"));
        when(contextManager.getMetaDataContexts().getMetaData().getDatabase("foo_db")).thenReturn(database);
        when(ProxyContext.getInstance().getContextManager()).thenReturn(contextManager);
    }
    
    private MySQLPacketPayload createPayload(final MySQLColumnDefinition41Packet packet) {
        ByteBuf byteBuf = Unpooled.buffer();
        packet.write(new MySQLPacketPayload(byteBuf, StandardCharsets.UTF_8));
        return new MySQLPacketPayload(byteBuf, StandardCharsets.UTF_8);
    }
    
    private void skipColumnDefinitionStrings(final MySQLPacketPayload payload) {
        payload.readStringLenenc();
        payload.readStringLenenc();
        payload.readStringLenenc();
        payload.readStringLenenc();
        payload.readStringLenenc();
        payload.readStringLenenc();
    }
    
    private static Stream<Arguments> columnTypes() {
        return Stream.of(
                Arguments.of(Types.CLOB, "BLOB SUB_TYPE TEXT", true, MySQLBinaryColumnType.BLOB, MySQLColumnDefinitionFlag.BLOB.getValue()),
                Arguments.of(Types.DATE, "YEAR", true, MySQLBinaryColumnType.YEAR, MySQLColumnDefinitionFlag.UNSIGNED.getValue()),
                Arguments.of(Types.SMALLINT, "year", true, MySQLBinaryColumnType.YEAR, MySQLColumnDefinitionFlag.UNSIGNED.getValue()),
                Arguments.of(Types.DATE, "DATE", true, MySQLBinaryColumnType.DATE, MySQLColumnDefinitionFlag.UNSIGNED.getValue()),
                Arguments.of(Types.SMALLINT, "SMALLINT", true, MySQLBinaryColumnType.SHORT, MySQLColumnDefinitionFlag.UNSIGNED.getValue()),
                Arguments.of(Types.DATE, "YEAR", false, MySQLBinaryColumnType.DATE, MySQLColumnDefinitionFlag.UNSIGNED.getValue()),
                Arguments.of(Types.SMALLINT, "YEAR", false, MySQLBinaryColumnType.SHORT, MySQLColumnDefinitionFlag.UNSIGNED.getValue()));
    }
    
    @Test
    void assertResolveProjectionPacketsWithLocalMetadata() throws SQLException {
        SelectStatementContext selectStatementContext = mock(SelectStatementContext.class, RETURNS_DEEP_STUBS);
        when(selectStatementContext.getTablesContext().getDatabaseName()).thenReturn(Optional.of("foo_db"));
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
        when(preparedStatement.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(3);
        mockResultSetMetaData(resultSetMetaData, 1, Types.DATE, "YEAR");
        when(resultSetMetaData.getColumnTypeName(2)).thenReturn("DATE");
        when(resultSetMetaData.getColumnTypeName(3)).thenReturn("VARCHAR");
        ConnectionSession connectionSession = mock(ConnectionSession.class);
        MySQLServerPreparedStatement serverPreparedStatement = mock(MySQLServerPreparedStatement.class);
        when(MySQLPreparedStatementMetadataFactory.load(connectionSession, serverPreparedStatement)).thenReturn(preparedStatement);
        mockDatabase();
        MySQLPacket localYear = mock(MySQLPacket.class);
        MySQLPacket localDate = mock(MySQLPacket.class);
        MySQLPacket localString = mock(MySQLPacket.class);
        Collection<MySQLPacket> localPackets = Arrays.asList(localYear, localDate, localString);
        Collection<MySQLPacket> actual = MySQLProjectionMetadataResolver.resolveProjectionPackets(connectionSession, serverPreparedStatement,
                selectStatementContext, MySQLCharacterSets.UTF8MB4_UNICODE_CI.getId(), true, localPackets);
        Iterator<MySQLPacket> actualIterator = actual.iterator();
        MySQLPacketPayload payload = createPayload((MySQLColumnDefinition41Packet) actualIterator.next());
        try {
            skipColumnDefinitionStrings(payload);
            payload.readIntLenenc();
            payload.skipReserved(6);
            assertThat(payload.readInt1(), is(MySQLBinaryColumnType.YEAR.getValue()));
        } finally {
            payload.getByteBuf().release();
        }
        assertThat(actualIterator.next(), sameInstance(localDate));
        assertThat(actualIterator.next(), sameInstance(localString));
        assertThat(localPackets.iterator().next(), sameInstance(localYear));
        verify(preparedStatement).close();
    }
    
    @Test
    void assertResolveProjectionPacketsWithoutMetadata() throws SQLException {
        SelectStatementContext selectStatementContext = mock(SelectStatementContext.class, RETURNS_DEEP_STUBS);
        when(selectStatementContext.getProjectionsContext().getExpandProjections()).thenReturn(Collections.singletonList(mock(Projection.class)));
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ConnectionSession connectionSession = mock(ConnectionSession.class);
        MySQLServerPreparedStatement serverPreparedStatement = mock(MySQLServerPreparedStatement.class);
        when(MySQLPreparedStatementMetadataFactory.load(connectionSession, serverPreparedStatement)).thenReturn(preparedStatement);
        Collection<MySQLPacket> actual = MySQLProjectionMetadataResolver.resolveProjectionPackets(connectionSession, serverPreparedStatement,
                selectStatementContext, MySQLCharacterSets.UTF8MB4_UNICODE_CI.getId(), true, Collections.emptyList());
        MySQLPacketPayload payload = createPayload((MySQLColumnDefinition41Packet) actual.iterator().next());
        try {
            skipColumnDefinitionStrings(payload);
            payload.readIntLenenc();
            payload.skipReserved(6);
            assertThat(payload.readInt1(), is(MySQLBinaryColumnType.VAR_STRING.getValue()));
        } finally {
            payload.getByteBuf().release();
        }
        verify(preparedStatement).close();
    }
    
    @Test
    void assertResolveProjectionPacketsWithLocalMetadataUnavailable() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ConnectionSession connectionSession = mock(ConnectionSession.class);
        MySQLServerPreparedStatement serverPreparedStatement = mock(MySQLServerPreparedStatement.class);
        when(MySQLPreparedStatementMetadataFactory.load(connectionSession, serverPreparedStatement)).thenReturn(preparedStatement);
        Collection<MySQLPacket> localPackets = Collections.singletonList(mock(MySQLPacket.class));
        Collection<MySQLPacket> actual = MySQLProjectionMetadataResolver.resolveProjectionPackets(connectionSession, serverPreparedStatement,
                mock(SelectStatementContext.class), MySQLCharacterSets.UTF8MB4_UNICODE_CI.getId(), true, localPackets);
        assertThat(actual, sameInstance(localPackets));
        verify(preparedStatement).close();
    }
    
}
