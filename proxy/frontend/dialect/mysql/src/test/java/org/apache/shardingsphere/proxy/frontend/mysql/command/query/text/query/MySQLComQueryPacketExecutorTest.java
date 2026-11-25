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

package org.apache.shardingsphere.proxy.frontend.mysql.command.query.text.query;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.apache.shardingsphere.database.protocol.mysql.constant.MySQLCharacterSets;
import org.apache.shardingsphere.database.protocol.mysql.constant.MySQLConstants;
import org.apache.shardingsphere.database.protocol.mysql.packet.command.query.text.MySQLTextResultSetRowPacket;
import org.apache.shardingsphere.database.protocol.mysql.packet.command.query.text.query.MySQLComQueryPacket;
import org.apache.shardingsphere.database.protocol.mysql.packet.generic.MySQLOKPacket;
import org.apache.shardingsphere.database.protocol.mysql.payload.MySQLPacketPayload;
import org.apache.shardingsphere.database.protocol.packet.DatabasePacket;
import org.apache.shardingsphere.proxy.backend.handler.ProxyBackendHandler;
import org.apache.shardingsphere.proxy.backend.response.data.QueryResponseCell;
import org.apache.shardingsphere.proxy.backend.response.data.QueryResponseRow;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.update.MultiStatementsUpdateResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.update.UpdateResponseHeader;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.proxy.frontend.command.executor.ResponseType;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.test.infra.framework.extension.mock.AutoMockExtension;
import org.apache.shardingsphere.test.infra.framework.extension.mock.StaticMockSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AutoMockExtension.class)
@StaticMockSettings(MySQLComQueryBackendHandlerFactory.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MySQLComQueryPacketExecutorTest {
    
    @Mock
    private ProxyBackendHandler proxyBackendHandler;
    
    @Mock
    private MySQLComQueryPacket packet;
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConnectionSession connectionSession;
    
    @BeforeEach
    void setUp() throws SQLException {
        when(connectionSession.getAttributeMap().attr(MySQLConstants.CHARACTER_SET_ATTRIBUTE_KEY).get()).thenReturn(MySQLCharacterSets.UTF8MB4_GENERAL_CI);
        when(MySQLComQueryBackendHandlerFactory.newInstance(packet, connectionSession)).thenReturn(proxyBackendHandler);
        when(proxyBackendHandler.getRowData()).thenReturn(new QueryResponseRow(Collections.emptyList()));
    }
    
    @Test
    void assertIsQueryResponse() throws SQLException {
        MySQLComQueryPacketExecutor mysqlComQueryPacketExecutor = new MySQLComQueryPacketExecutor(packet, connectionSession);
        QueryHeader queryHeader = mock(QueryHeader.class);
        when(queryHeader.getColumnTypeName()).thenReturn("VARCHAR");
        when(proxyBackendHandler.execute()).thenReturn(new QueryResponseHeader(Collections.singletonList(queryHeader)));
        mysqlComQueryPacketExecutor.execute();
        assertThat(mysqlComQueryPacketExecutor.getResponseType(), is(ResponseType.QUERY));
    }
    
    @Test
    void assertExecuteUpdateResponse() throws SQLException {
        MySQLComQueryPacketExecutor actual = new MySQLComQueryPacketExecutor(packet, connectionSession);
        when(proxyBackendHandler.execute()).thenReturn(new UpdateResponseHeader(mock(SQLStatement.class)));
        Collection<DatabasePacket> actualPackets = actual.execute();
        assertThat(actual.getResponseType(), is(ResponseType.UPDATE));
        assertThat(actualPackets.size(), is(1));
        assertThat(actualPackets.iterator().next(), isA(MySQLOKPacket.class));
    }
    
    @Test
    void assertExecuteMultiStatementsUpdateResponse() throws SQLException {
        MySQLComQueryPacketExecutor actual = new MySQLComQueryPacketExecutor(packet, connectionSession);
        when(proxyBackendHandler.execute())
                .thenReturn(new MultiStatementsUpdateResponseHeader(Arrays.asList(new UpdateResponseHeader(mock(SQLStatement.class)), new UpdateResponseHeader(mock(SQLStatement.class)))));
        Collection<DatabasePacket> actualPackets = actual.execute();
        assertThat(actualPackets.size(), is(2));
        Iterator<DatabasePacket> iterator = actualPackets.iterator();
        assertThat(iterator.next(), isA(MySQLOKPacket.class));
        assertThat(iterator.next(), isA(MySQLOKPacket.class));
    }
    
    @Test
    void assertNext() throws SQLException {
        MySQLComQueryPacketExecutor actual = new MySQLComQueryPacketExecutor(packet, connectionSession);
        when(proxyBackendHandler.next()).thenReturn(true, false);
        assertTrue(actual.next());
        assertFalse(actual.next());
    }
    
    @Test
    void assertGetQueryRowPacket() throws SQLException {
        assertThat(new MySQLComQueryPacketExecutor(packet, connectionSession).getQueryRowPacket(), isA(MySQLTextResultSetRowPacket.class));
    }
    
    @ParameterizedTest(name = "{0}: {1}: {2}")
    @MethodSource("textColumnValues")
    void assertGetQueryRowPacketWithYear(final int jdbcType, final String columnTypeName, final Object value, final byte[] expected) throws SQLException {
        when(proxyBackendHandler.getRowData()).thenReturn(new QueryResponseRow(Arrays.asList(new QueryResponseCell(jdbcType, value, columnTypeName), new QueryResponseCell(Types.INTEGER, 0))));
        DatabasePacket actual = new MySQLComQueryPacketExecutor(packet, connectionSession).getQueryRowPacket();
        ByteBuf buffer = Unpooled.buffer();
        try {
            actual.write(new MySQLPacketPayload(buffer, StandardCharsets.UTF_8));
            assertThat(ByteBufUtil.getBytes(buffer), is(expected));
        } finally {
            buffer.release();
        }
    }
    
    private static Stream<Arguments> textColumnValues() {
        return Stream.of(
                Arguments.of(Types.INTEGER, "YEAR", 0, new byte[]{4, '0', '0', '0', '0', 1, '0'}),
                Arguments.of(Types.INTEGER, "YEAR", 2000, new byte[]{4, '2', '0', '0', '0', 1, '0'}),
                Arguments.of(Types.INTEGER, "year", 2023, new byte[]{4, '2', '0', '2', '3', 1, '0'}),
                Arguments.of(Types.INTEGER, "YEAR", null, new byte[]{(byte) 0xfb, 1, '0'}),
                Arguments.of(Types.INTEGER, "INTEGER", 0, new byte[]{1, '0', 1, '0'}),
                Arguments.of(Types.OTHER, "foo_type", 0, new byte[]{1, '0', 1, '0'}),
                Arguments.of(Types.SMALLINT, "YEAR", (short) 0, new byte[]{1, '0', 1, '0'}),
                Arguments.of(Types.DATE, "YEAR", Date.valueOf("2000-01-01"), new byte[]{10, '2', '0', '0', '0', '-', '0', '1', '-', '0', '1', 1, '0'}));
    }
    
    @Test
    void assertClose() throws SQLException {
        MySQLComQueryPacketExecutor actual = new MySQLComQueryPacketExecutor(packet, connectionSession);
        actual.close();
        verify(proxyBackendHandler).close();
    }
}
