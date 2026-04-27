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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MCPResourceSpecificationFactoryTest {
    
    @Test
    void assertCreateResourceSpecifications() {
        MCPResourceSpecificationFactory factory = createFactory();
        List<SyncResourceSpecification> actual = factory.createResourceSpecifications();
        List<String> actualResourceUris = actual.stream().map(each -> each.resource().uri()).toList();
        assertTrue(actualResourceUris.stream().noneMatch(each -> each.contains("{")));
        assertTrue(actualResourceUris.contains("shardingsphere://capabilities"));
        assertTrue(actualResourceUris.contains("shardingsphere://features/encrypt/algorithms"));
        assertTrue(actualResourceUris.contains("shardingsphere://features/mask/algorithms"));
        SyncResourceSpecification actualSpecification = findResourceSpecification(actual, "shardingsphere://capabilities");
        assertThat(actualSpecification.resource().name(), is("capabilities"));
        assertThat(actualSpecification.resource().description(), is("ShardingSphere MCP resource: shardingsphere://capabilities"));
        assertThat(actualSpecification.resource().mimeType(), is("application/json"));
        assertNotNull(actualSpecification.readHandler());
    }
    
    @Test
    void assertCreateResourceTemplateSpecifications() {
        MCPResourceSpecificationFactory factory = createFactory();
        List<SyncResourceTemplateSpecification> actual = factory.createResourceTemplateSpecifications();
        List<String> actualResourceUriTemplates = actual.stream().map(each -> each.resourceTemplate().uriTemplate()).toList();
        assertTrue(actualResourceUriTemplates.stream().allMatch(each -> each.contains("{")));
        assertTrue(actualResourceUriTemplates.contains("shardingsphere://databases/{database}"));
        assertTrue(actualResourceUriTemplates.contains("shardingsphere://features/encrypt/databases/{database}/rules"));
        assertTrue(actualResourceUriTemplates.contains("shardingsphere://features/mask/databases/{database}/rules"));
        SyncResourceTemplateSpecification actualSpecification = findResourceTemplateSpecification(actual, "shardingsphere://databases/{database}");
        assertThat(actualSpecification.resourceTemplate().name(), is("{database}"));
        assertThat(actualSpecification.resourceTemplate().description(), is("ShardingSphere MCP resource template: shardingsphere://databases/{database}"));
        assertThat(actualSpecification.resourceTemplate().mimeType(), is("application/json"));
        assertNotNull(actualSpecification.readHandler());
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
