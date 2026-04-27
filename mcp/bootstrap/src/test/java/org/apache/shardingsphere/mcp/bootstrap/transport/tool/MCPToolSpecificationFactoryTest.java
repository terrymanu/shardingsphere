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

package org.apache.shardingsphere.mcp.bootstrap.transport.tool;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import org.apache.shardingsphere.mcp.context.MCPRuntimeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MCPToolSpecificationFactoryTest {
    
    @Test
    void assertCreateToolSpecificationsContainsExpectedBaselineToolNames() {
        MCPToolSpecificationFactory factory = createFactory();
        List<SyncToolSpecification> actual = factory.createToolSpecifications();
        assertTrue(actual.stream().map(each -> each.tool().name()).collect(Collectors.toSet()).containsAll(Set.of(
                "search_metadata", "execute_query", "plan_encrypt_rule", "validate_encrypt_rule",
                "apply_encrypt_rule", "plan_mask_rule", "validate_mask_rule", "apply_mask_rule")));
    }
    
    @ParameterizedTest(name = "{0}")
    @MethodSource("assertCreateToolSpecificationsArguments")
    void assertCreateToolSpecificationsWithSchema(final String name, final String toolName,
                                                  final String expectedTitle, final String fieldName, final boolean expectedRequired, final Map<String, Object> expectedProperty) {
        MCPToolSpecificationFactory factory = createFactory();
        List<SyncToolSpecification> actual = factory.createToolSpecifications();
        SyncToolSpecification actualSpecification = findToolSpecification(actual, toolName);
        assertThat(actualSpecification.tool().title(), is(expectedTitle));
        assertThat(actualSpecification.tool().description(), is("ShardingSphere MCP tool: " + toolName));
        assertThat(actualSpecification.tool().inputSchema().type(), is("object"));
        assertTrue(actualSpecification.tool().inputSchema().additionalProperties());
        assertThat(actualSpecification.tool().inputSchema().required().contains(fieldName), is(expectedRequired));
        assertThat(actualSpecification.tool().inputSchema().properties().get(fieldName), is(expectedProperty));
        assertNotNull(actualSpecification.callHandler());
    }
    
    @ParameterizedTest(name = "{0}")
    @MethodSource("assertCreateToolSpecificationsWithoutRemovedFieldsArguments")
    void assertCreateToolSpecificationsWithoutRemovedFields(final String name, final String toolName, final String fieldName) {
        MCPToolSpecificationFactory factory = createFactory();
        List<SyncToolSpecification> actual = factory.createToolSpecifications();
        SyncToolSpecification actualSpecification = findToolSpecification(actual, toolName);
        assertFalse(actualSpecification.tool().inputSchema().properties().containsKey(fieldName));
        assertFalse(actualSpecification.tool().inputSchema().required().contains(fieldName));
    }
    
    private SyncToolSpecification findToolSpecification(final List<SyncToolSpecification> specifications, final String toolName) {
        return specifications.stream().filter(each -> toolName.equals(each.tool().name())).findFirst().orElseThrow(IllegalStateException::new);
    }
    
    private MCPToolSpecificationFactory createFactory() {
        return new MCPToolSpecificationFactory(mock(MCPRuntimeContext.class));
    }
    
    private static Stream<Arguments> assertCreateToolSpecificationsArguments() {
        return Stream.of(
                Arguments.of("search metadata query field", "search_metadata", "Search Metadata", "query", true,
                        Map.of("type", "string", "description", "Search query.")),
                Arguments.of("search metadata object types field", "search_metadata", "Search Metadata", "object_types", false,
                        Map.of("type", "array", "description", "Optional object-type filter. Allowed values: database, schema, table, view, column, index, sequence.",
                                "items", Map.of("type", "string", "description", "Allowed values: database, schema, table, view, column, index, sequence."))),
                Arguments.of("execute query timeout field", "execute_query", "Execute Query", "timeout_ms", false,
                        Map.of("type", "integer", "description", "Optional timeout in milliseconds.")),
                Arguments.of("plan encrypt rule algorithm type field", "plan_encrypt_rule", "Plan Encrypt Rule", "algorithm_type", false,
                        Map.of("type", "string", "description", "Primary algorithm type override.")),
                Arguments.of("plan mask rule algorithm type field", "plan_mask_rule", "Plan Mask Rule", "algorithm_type", false,
                        Map.of("type", "string", "description", "Primary mask algorithm type override.")));
    }
    
    private static Stream<Arguments> assertCreateToolSpecificationsWithoutRemovedFieldsArguments() {
        return Stream.of(
                Arguments.of("plan encrypt rule excludes raw user request", "plan_encrypt_rule", "raw_user_request"),
                Arguments.of("plan encrypt rule excludes sample data flag", "plan_encrypt_rule", "allow_sample_data"),
                Arguments.of("plan mask rule excludes raw user request", "plan_mask_rule", "raw_user_request"),
                Arguments.of("plan mask rule excludes sample data flag", "plan_mask_rule", "allow_sample_data"));
    }
}
