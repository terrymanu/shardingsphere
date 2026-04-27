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

package org.apache.shardingsphere.mcp.bootstrap.transport.server.http;

import org.apache.shardingsphere.infra.util.json.JsonUtils;
import org.apache.shardingsphere.mcp.bootstrap.transport.MCPTransportConstants;
import org.apache.shardingsphere.mcp.metadata.jdbc.RuntimeDatabaseConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamableHttpTransportIT extends AbstractStreamableHttpIT {
    
    private static final String DATABASE_TYPE = "MySQL";
    
    private static final String DATABASE_VERSION = "8.0.36";
    
    private static final String JDBC_URL = "jdbc:mysql://bootstrap-http/test";
    
    @Test
    void assertLaunchHttpServerWithConfiguredEndpoint() throws IOException, InterruptedException, SQLException {
        launchJDBCRuntime();
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> initializeResponse = sendInitializeRequest(httpClient, createJsonRequestHeaders(), createInitializeRequestParams("integration-test"));
        assertThat(initializeResponse.statusCode(), is(200));
        assertTrue(initializeResponse.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertThat(initializeResponse.headers().firstValue("MCP-Protocol-Version").orElse(""), is(MCPTransportConstants.PROTOCOL_VERSION));
        Map<String, Object> initializePayload = parseJsonBody(initializeResponse.body());
        assertThat(initializePayload.get("jsonrpc"), is("2.0"));
        Map<String, Object> result = castToMap(initializePayload.get("result"));
        assertThat(result.get("protocolVersion"), is(MCPTransportConstants.PROTOCOL_VERSION));
        String sessionId = initializeResponse.headers().firstValue("MCP-Session-Id").orElse("");
        assertFalse(sessionId.isEmpty());
        HttpResponse<String> toolCallResponse = sendToolCallRequest(httpClient, sessionId, "search_metadata",
                Map.of("database", "logic_db", "query", "order", "object_types", List.of("table")));
        assertThat(toolCallResponse.statusCode(), is(200));
        assertTrue(getPayloadItems(getStructuredContent(toolCallResponse.body())).stream().anyMatch(each -> "orders".equals(each.get("name"))));
        HttpResponse<String> resourceReadResponse = sendCapabilitiesRequest(httpClient, createJsonRequestHeaders(Map.of(
                "MCP-Session-Id", sessionId,
                "MCP-Protocol-Version", MCPTransportConstants.PROTOCOL_VERSION)));
        assertThat(resourceReadResponse.statusCode(), is(200));
        assertTrue(getResourcePayload(resourceReadResponse.body()).containsKey("supportedResources"));
        HttpResponse<String> deleteResponse = sendDeleteRequest(httpClient, Map.of("MCP-Session-Id", sessionId, "MCP-Protocol-Version", MCPTransportConstants.PROTOCOL_VERSION));
        assertThat(deleteResponse.statusCode(), is(200));
    }
    
    @Test
    void assertAcceptFollowUpRequestWithLowercaseHeaders() throws IOException, InterruptedException, SQLException {
        RuntimeHttpSession session = launchRuntime();
        HttpResponse<String> actualResponse = sendCapabilitiesRequest(session.httpClient(), createJsonRequestHeaders(Map.of(
                "mcp-session-id", session.sessionId(),
                "mcp-protocol-version", MCPTransportConstants.PROTOCOL_VERSION)));
        assertSuccessfulCapabilitiesResponse(actualResponse);
    }
    
    @Test
    void assertAcceptInitializeWithAccessToken() throws IOException, InterruptedException, SQLException {
        launchJDBCRuntime("127.0.0.1", false, ACCESS_TOKEN);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> actualResponse = sendInitializeRequest(httpClient,
                createJsonRequestHeaders(Map.of("Authorization", getAuthorizationHeaderValue(ACCESS_TOKEN))), createInitializeRequestParams("integration-test"));
        assertThat(actualResponse.statusCode(), is(200));
        assertFalse(actualResponse.headers().firstValue("MCP-Session-Id").orElse("").isEmpty());
    }
    
    @Test
    void assertRejectInitializeWithoutAccessToken() throws IOException, InterruptedException, SQLException {
        launchJDBCRuntime("127.0.0.1", false, ACCESS_TOKEN);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> actualResponse = sendInitializeRequest(httpClient, createJsonRequestHeaders(), createInitializeRequestParams("integration-test"));
        assertThat(actualResponse.statusCode(), is(401));
        assertThat(parseJsonBody(actualResponse.body()).get("message"), is("Unauthorized."));
    }
    
    @Test
    void assertRejectInitializeWithWrongAccessToken() throws IOException, InterruptedException, SQLException {
        launchJDBCRuntime("127.0.0.1", false, ACCESS_TOKEN);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> actualResponse = sendInitializeRequest(httpClient,
                createJsonRequestHeaders(Map.of("Authorization", getAuthorizationHeaderValue("wrong-token"))), createInitializeRequestParams("integration-test"));
        assertThat(actualResponse.statusCode(), is(401));
        assertThat(parseJsonBody(actualResponse.body()).get("message"), is("Unauthorized."));
    }
    
    @Test
    void assertRejectOpenStreamAfterDelete() throws IOException, InterruptedException, SQLException {
        RuntimeHttpSession session = launchRuntime();
        sendDeleteRequest(session.httpClient(), Map.of("MCP-Session-Id", session.sessionId(), "MCP-Protocol-Version", MCPTransportConstants.PROTOCOL_VERSION));
        HttpResponse<String> streamResponse = openEventStream(session.httpClient(), Map.of(
                "Accept", "text/event-stream",
                "MCP-Session-Id", session.sessionId(),
                "MCP-Protocol-Version", MCPTransportConstants.PROTOCOL_VERSION));
        assertThat(streamResponse.statusCode(), is(404));
        assertThat(parseJsonBody(streamResponse.body()).get("message"), is("Session does not exist."));
    }
    
    @Test
    void assertAcceptInitializeWithIpv6LoopbackOrigin() throws IOException, InterruptedException, SQLException {
        launchJDBCRuntime();
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> initializeResponse = sendInitializeRequest(httpClient,
                createJsonRequestHeaders(Map.of("Origin", "http://[::1]:8080")), createInitializeRequestParams("integration-test"));
        assertThat(initializeResponse.statusCode(), is(200));
        assertFalse(initializeResponse.headers().firstValue("MCP-Session-Id").orElse("").isEmpty());
    }
    
    @Test
    void assertRejectInitializeWithInvalidOrigin() throws IOException, InterruptedException, SQLException {
        launchJDBCRuntime();
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> initializeResponse = sendInitializeRequest(httpClient,
                createJsonRequestHeaders(Map.of("Origin", "https://evil.example.com")), createInitializeRequestParams("integration-test"));
        assertThat(initializeResponse.statusCode(), is(403));
        assertThat(parseJsonBody(initializeResponse.body()).get("message"), is("Origin is not allowed for the current binding."));
    }
    
    @Test
    void assertAcceptInitializeWithUnsupportedProtocolVersion() throws IOException, InterruptedException, SQLException {
        launchJDBCRuntime();
        HttpClient httpClient = HttpClient.newHttpClient();
        Map<String, Object> requestParams = createInitializeRequestParams("integration-test");
        requestParams.put("protocolVersion", "2024-11-05");
        HttpResponse<String> initializeResponse = sendInitializeRequest(httpClient, createJsonRequestHeaders(), requestParams);
        String sessionId = initializeResponse.headers().firstValue("MCP-Session-Id").orElse("");
        String protocolVersion = initializeResponse.headers().firstValue("MCP-Protocol-Version").orElse("");
        assertThat(initializeResponse.statusCode(), is(200));
        assertFalse(sessionId.isEmpty());
        assertThat(protocolVersion, is(MCPTransportConstants.PROTOCOL_VERSION));
        assertThat(castToMap(parseJsonBody(initializeResponse.body()).get("result")).get("protocolVersion"), is(MCPTransportConstants.PROTOCOL_VERSION));
        HttpResponse<String> actualResponse = sendCapabilitiesRequest(httpClient, createJsonRequestHeaders(Map.of(
                "MCP-Session-Id", sessionId,
                "MCP-Protocol-Version", protocolVersion)));
        assertSuccessfulCapabilitiesResponse(actualResponse);
    }
    
    @Test
    void assertAcceptInitializeWithoutProtocolVersion() throws IOException, InterruptedException, SQLException {
        launchJDBCRuntime();
        HttpClient httpClient = HttpClient.newHttpClient();
        Map<String, Object> requestParams = createInitializeRequestParams("integration-test");
        requestParams.remove("protocolVersion");
        HttpResponse<String> initializeResponse = sendInitializeRequest(httpClient, createJsonRequestHeaders(), requestParams);
        String sessionId = initializeResponse.headers().firstValue("MCP-Session-Id").orElse("");
        String protocolVersion = initializeResponse.headers().firstValue("MCP-Protocol-Version").orElse("");
        assertThat(initializeResponse.statusCode(), is(200));
        assertFalse(sessionId.isEmpty());
        assertThat(protocolVersion, is(MCPTransportConstants.PROTOCOL_VERSION));
        assertThat(castToMap(parseJsonBody(initializeResponse.body()).get("result")).get("protocolVersion"), is(MCPTransportConstants.PROTOCOL_VERSION));
        HttpResponse<String> actualResponse = sendCapabilitiesRequest(httpClient, createJsonRequestHeaders(Map.of(
                "MCP-Session-Id", sessionId,
                "MCP-Protocol-Version", protocolVersion)));
        assertSuccessfulCapabilitiesResponse(actualResponse);
    }
    
    @Test
    void assertRejectFollowUpRequestWithoutAccessTokenBeforeSessionValidation() throws IOException, InterruptedException, SQLException {
        RuntimeHttpSession session = launchRuntimeWithAccessToken();
        HttpResponse<String> actualResponse = sendCapabilitiesRequest(session.httpClient(), createJsonRequestHeaders(Map.of(
                "MCP-Session-Id", "missing-session",
                "MCP-Protocol-Version", MCPTransportConstants.PROTOCOL_VERSION)));
        assertThat(actualResponse.statusCode(), is(401));
        assertThat(parseJsonBody(actualResponse.body()).get("message"), is("Unauthorized."));
    }
    
    @Test
    void assertRejectFollowUpRequestWithWrongAccessToken() throws IOException, InterruptedException, SQLException {
        RuntimeHttpSession session = launchRuntimeWithAccessToken();
        HttpResponse<String> actualResponse = sendCapabilitiesRequest(session.httpClient(), createJsonRequestHeaders(Map.of(
                "Authorization", getAuthorizationHeaderValue("wrong-token"),
                "MCP-Session-Id", session.sessionId(),
                "MCP-Protocol-Version", MCPTransportConstants.PROTOCOL_VERSION)));
        assertThat(actualResponse.statusCode(), is(401));
        assertThat(parseJsonBody(actualResponse.body()).get("message"), is("Unauthorized."));
    }
    
    @Test
    void assertRejectFollowUpRequestWithoutProtocolHeader() throws IOException, InterruptedException, SQLException {
        RuntimeHttpSession session = launchRuntime();
        HttpResponse<String> actualResponse = sendCapabilitiesRequest(session.httpClient(), createJsonRequestHeaders(Map.of("MCP-Session-Id", session.sessionId())));
        assertThat(actualResponse.statusCode(), is(400));
        assertThat(parseJsonBody(actualResponse.body()).get("message"), is("MCP-Protocol-Version header is required."));
    }
    
    @Test
    void assertRejectDeleteForClosedSession() throws IOException, InterruptedException, SQLException {
        RuntimeHttpSession session = launchRuntime();
        HttpResponse<String> deleteResponse = sendDeleteRequest(session.httpClient(), session.sessionId());
        HttpResponse<String> actualResponse = sendDeleteRequest(session.httpClient(), session.sessionId());
        assertThat(deleteResponse.statusCode(), is(200));
        assertThat(actualResponse.statusCode(), is(404));
        assertThat(parseJsonBody(actualResponse.body()).get("message"), is("Session does not exist."));
    }
    
    @Test
    void assertCloseSessionWithAccessToken() throws IOException, InterruptedException, SQLException {
        RuntimeHttpSession session = launchRuntimeWithAccessToken();
        HttpResponse<String> actualResponse = sendDeleteRequest(session.httpClient(), Map.of(
                "Authorization", getAuthorizationHeaderValue(session.accessToken()),
                "MCP-Session-Id", session.sessionId(),
                "MCP-Protocol-Version", MCPTransportConstants.PROTOCOL_VERSION));
        assertThat(actualResponse.statusCode(), is(200));
    }
    
    @Override
    protected Map<String, RuntimeDatabaseConfiguration> createRuntimeDatabases() {
        return Map.of("logic_db", createRuntimeDatabaseConfiguration());
    }
    
    private RuntimeDatabaseConfiguration createRuntimeDatabaseConfiguration() {
        try {
            Connection connection = createMetadataConnection();
            RuntimeDatabaseConfiguration result = mock(RuntimeDatabaseConfiguration.class);
            when(result.getDatabaseType()).thenReturn(DATABASE_TYPE);
            when(result.openConnection(anyString())).thenReturn(connection);
            return result;
        } catch (final SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    private Connection createMetadataConnection() throws SQLException {
        Connection result = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        Map<String, List<String>> columns = Map.of(
                "orders", List.of("amount", "order_id", "status"),
                "order_items", List.of("item_id", "order_id", "sku"));
        when(result.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn(DATABASE_TYPE);
        when(databaseMetaData.getDatabaseProductVersion()).thenReturn(DATABASE_VERSION);
        when(databaseMetaData.getURL()).thenReturn(JDBC_URL);
        when(databaseMetaData.getTables(isNull(), isNull(), eq("%"), any(String[].class))).thenAnswer(invocation -> {
            String[] tableTypes = invocation.getArgument(3);
            return "TABLE".equals(tableTypes[0])
                    ? mockMultiRowResultSet(List.of(Map.of("TABLE_NAME", "orders"), Map.of("TABLE_NAME", "order_items")))
                    : mockMultiRowResultSet(List.of());
        });
        when(databaseMetaData.getColumns(isNull(), isNull(), anyString(), eq("%"))).thenAnswer(invocation -> {
            String objectName = invocation.getArgument(2);
            return mockResultSet("COLUMN_NAME", columns.getOrDefault(objectName, List.of()).toArray(new String[0]));
        });
        when(databaseMetaData.getIndexInfo(isNull(), isNull(), anyString(), eq(false), eq(false))).thenAnswer(invocation -> mockResultSet("INDEX_NAME"));
        return result;
    }
    
    private ResultSet mockResultSet(final String columnName, final String... values) throws SQLException {
        ResultSet result = mock(ResultSet.class);
        AtomicInteger nextIndex = new AtomicInteger();
        AtomicInteger valueIndex = new AtomicInteger();
        when(result.next()).thenAnswer(invocation -> nextIndex.getAndIncrement() < values.length);
        when(result.getString(columnName)).thenAnswer(invocation -> values[valueIndex.getAndIncrement()]);
        return result;
    }
    
    private ResultSet mockMultiRowResultSet(final List<Map<String, String>> values) throws SQLException {
        ResultSet result = mock(ResultSet.class);
        AtomicInteger rowIndex = new AtomicInteger(-1);
        when(result.next()).thenAnswer(invocation -> rowIndex.incrementAndGet() < values.size());
        when(result.getString(anyString())).thenAnswer(invocation -> {
            int currentRowIndex = rowIndex.get();
            return 0 <= currentRowIndex && currentRowIndex < values.size() ? values.get(currentRowIndex).get(invocation.getArgument(0)) : null;
        });
        return result;
    }
    
    private HttpResponse<String> sendInitializeRequest(final HttpClient httpClient, final Map<String, String> requestHeaders,
                                                       final Map<String, Object> requestParams) throws IOException, InterruptedException {
        return sendPostRequest(httpClient, requestHeaders, Map.of("jsonrpc", "2.0", "id", "init-1", "method", "initialize", "params", requestParams));
    }
    
    private HttpResponse<String> sendCapabilitiesRequest(final HttpClient httpClient, final Map<String, String> requestHeaders) throws IOException, InterruptedException {
        return sendPostRequest(httpClient, requestHeaders, Map.of(
                "jsonrpc", "2.0",
                "id", "resource-1",
                "method", "resources/read",
                "params", Map.of("uri", "shardingsphere://capabilities")));
    }
    
    private HttpResponse<String> sendDeleteRequest(final HttpClient httpClient, final Map<String, String> requestHeaders) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(createEndpointUri()).DELETE();
        requestHeaders.forEach(requestBuilder::header);
        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }
    
    private HttpResponse<String> openEventStream(final HttpClient httpClient, final Map<String, String> requestHeaders) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(createEndpointUri()).GET();
        requestHeaders.forEach(requestBuilder::header);
        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }
    
    private HttpResponse<String> sendPostRequest(final HttpClient httpClient, final Map<String, String> requestHeaders,
                                                 final Map<String, Object> requestBody) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(createEndpointUri())
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJsonString(requestBody)));
        requestHeaders.forEach(requestBuilder::header);
        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }
    
    private void assertSuccessfulCapabilitiesResponse(final HttpResponse<String> actualResponse) {
        assertThat(actualResponse.statusCode(), is(200));
        assertTrue(getResourcePayload(actualResponse.body()).containsKey("supportedTools"));
    }
    
}
