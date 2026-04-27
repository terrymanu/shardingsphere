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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MCPToolSpecificationFactoryTest {
    
    @Test
    void assertCreateToolSpecificationsContainsExpectedToolNames() {
        MCPToolSpecificationFactory factory = createFactory();
        List<SyncToolSpecification> actual = factory.createToolSpecifications();
        assertTrue(actual.stream().map(each -> each.tool().name()).collect(Collectors.toSet()).containsAll(Set.of(
                "search_metadata", "execute_query", "plan_encrypt_rule", "validate_encrypt_rule",
                "apply_encrypt_rule", "plan_mask_rule", "validate_mask_rule", "apply_mask_rule")));
    }
    
    @Test
    void assertCreateToolSpecificationsWithSearchMetadataSchema() {
        MCPToolSpecificationFactory factory = createFactory();
        List<SyncToolSpecification> actual = factory.createToolSpecifications();
        SyncToolSpecification actualSpecification = findToolSpecification(actual, "search_metadata");
        assertThat(actualSpecification.tool().title(), is("Search Metadata"));
        assertThat(actualSpecification.tool().description(), is("ShardingSphere MCP tool: search_metadata"));
        assertThat(actualSpecification.tool().inputSchema().type(), is("object"));
        assertTrue(actualSpecification.tool().inputSchema().additionalProperties());
        assertTrue(actualSpecification.tool().inputSchema().required().contains("query"));
        assertFalse(actualSpecification.tool().inputSchema().required().contains("object_types"));
        assertThat(actualSpecification.tool().inputSchema().properties().get("query"), is(Map.of("type", "string", "description", "Search query.")));
        assertThat(actualSpecification.tool().inputSchema().properties().get("object_types"), is(Map.of(
                "type", "array",
                "description", "Optional object-type filter. Allowed values: database, schema, table, view, column, index, sequence.",
                "items", Map.of("type", "string", "description", "Allowed values: database, schema, table, view, column, index, sequence."))));
        assertNotNull(actualSpecification.callHandler());
    }
    
    private SyncToolSpecification findToolSpecification(final List<SyncToolSpecification> specifications, final String toolName) {
        return specifications.stream().filter(each -> toolName.equals(each.tool().name())).findFirst().orElseThrow(IllegalStateException::new);
    }
    
    private MCPToolSpecificationFactory createFactory() {
        return new MCPToolSpecificationFactory(mock(MCPRuntimeContext.class));
    }
}
