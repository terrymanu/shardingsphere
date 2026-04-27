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

package org.apache.shardingsphere.mcp.bootstrap.transport.resource;

import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification;
import org.apache.shardingsphere.mcp.context.MCPRuntimeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MCPResourceSpecificationFactoryTest {
    
    @Test
    void assertCreateResourceSpecificationsContainsExpectedBaselineResources() {
        MCPResourceSpecificationFactory factory = createFactory();
        List<SyncResourceSpecification> actual = factory.createResourceSpecifications();
        List<String> actualResourceUris = actual.stream().map(each -> each.resource().uri()).toList();
        assertTrue(actualResourceUris.stream().noneMatch(each -> each.contains("{")));
        assertTrue(actualResourceUris.contains("shardingsphere://capabilities"));
        assertTrue(actualResourceUris.contains("shardingsphere://databases"));
        assertTrue(actualResourceUris.contains("shardingsphere://features/encrypt/algorithms"));
        assertTrue(actualResourceUris.contains("shardingsphere://features/mask/algorithms"));
        SyncResourceSpecification actualSpecification = findResourceSpecification(actual, "shardingsphere://capabilities");
        assertThat(actualSpecification.resource().name(), is("capabilities"));
        assertThat(actualSpecification.resource().description(), is("ShardingSphere MCP resource: shardingsphere://capabilities"));
        assertThat(actualSpecification.resource().mimeType(), is("application/json"));
        assertNotNull(actualSpecification.readHandler());
    }
    
    @Test
    void assertCreateResourceTemplateSpecificationsContainsFeatureUris() {
        MCPResourceSpecificationFactory factory = createFactory();
        List<SyncResourceTemplateSpecification> actual = factory.createResourceTemplateSpecifications();
        assertTrue(actual.stream().map(each -> each.resourceTemplate().uriTemplate()).toList().contains("shardingsphere://features/encrypt/databases/{database}/rules"));
        assertTrue(actual.stream().map(each -> each.resourceTemplate().uriTemplate()).toList()
                .contains("shardingsphere://features/encrypt/databases/{database}/tables/{table}/rules"));
        assertTrue(actual.stream().map(each -> each.resourceTemplate().uriTemplate()).toList().contains("shardingsphere://features/mask/databases/{database}/rules"));
        assertTrue(actual.stream().map(each -> each.resourceTemplate().uriTemplate()).toList()
                .contains("shardingsphere://features/mask/databases/{database}/tables/{table}/rules"));
    }
    
    @ParameterizedTest(name = "{0}")
    @MethodSource("assertCreateResourceTemplateSpecificationsArguments")
    void assertCreateResourceTemplateSpecifications(final String name, final String uriTemplate, final String expectedName) {
        MCPResourceSpecificationFactory factory = createFactory();
        List<SyncResourceTemplateSpecification> actual = factory.createResourceTemplateSpecifications();
        assertTrue(actual.stream().allMatch(each -> each.resourceTemplate().uriTemplate().contains("{")));
        SyncResourceTemplateSpecification actualSpecification = findResourceTemplateSpecification(actual, uriTemplate);
        assertThat(actualSpecification.resourceTemplate().name(), is(expectedName));
        assertThat(actualSpecification.resourceTemplate().description(), is("ShardingSphere MCP resource template: " + uriTemplate));
        assertThat(actualSpecification.resourceTemplate().mimeType(), is("application/json"));
        assertNotNull(actualSpecification.readHandler());
    }
    
    private static Stream<Arguments> assertCreateResourceTemplateSpecificationsArguments() {
        return Stream.of(
                Arguments.of("database resource", "shardingsphere://databases/{database}", "{database}"),
                Arguments.of("schema resource", "shardingsphere://databases/{database}/schemas/{schema}", "{schema}"),
                Arguments.of("sequence resource", "shardingsphere://databases/{database}/schemas/{schema}/sequences/{sequence}", "{sequence}"));
    }
    
    private SyncResourceSpecification findResourceSpecification(final List<SyncResourceSpecification> specifications, final String uri) {
        return specifications.stream().filter(each -> uri.equals(each.resource().uri())).findFirst().orElseThrow(IllegalStateException::new);
    }
    
    private SyncResourceTemplateSpecification findResourceTemplateSpecification(final List<SyncResourceTemplateSpecification> specifications, final String uriTemplate) {
        return specifications.stream().filter(each -> uriTemplate.equals(each.resourceTemplate().uriTemplate())).findFirst().orElseThrow(IllegalStateException::new);
    }
    
    private MCPResourceSpecificationFactory createFactory() {
        return new MCPResourceSpecificationFactory(mock(MCPRuntimeContext.class));
    }
}
