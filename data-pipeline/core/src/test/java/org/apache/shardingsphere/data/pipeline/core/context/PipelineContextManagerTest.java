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

package org.apache.shardingsphere.data.pipeline.core.context;

import org.apache.shardingsphere.infra.instance.metadata.InstanceType;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PipelineContextManagerTest {
    
    private final PipelineContextKey fooContextKey = new PipelineContextKey("foo_pipeline_context_manager_test", InstanceType.JDBC);
    
    private final PipelineContextKey barContextKey = new PipelineContextKey("bar_pipeline_context_manager_test", InstanceType.JDBC);
    
    @AfterEach
    void tearDown() {
        PipelineContextManager.removeContext(fooContextKey);
        PipelineContextManager.removeContext(barContextKey);
    }
    
    @Test
    void assertGetContext() {
        assertNull(PipelineContextManager.getContext(fooContextKey));
        ContextManager expected = mock(ContextManager.class);
        ContextManager expectedOtherContextManager = mock(ContextManager.class);
        PipelineContextManager.putContext(fooContextKey, expected);
        PipelineContextManager.putContext(barContextKey, expectedOtherContextManager);
        assertThat(PipelineContextManager.getContext(fooContextKey), sameInstance(expected));
        PipelineContextManager.removeContext(fooContextKey);
        assertNull(PipelineContextManager.getContext(fooContextKey));
        assertThat(PipelineContextManager.getContext(barContextKey), sameInstance(expectedOtherContextManager));
    }
    
    @Test
    void assertGetProxyContext() {
        PipelineContextKey proxyContextKey = new PipelineContextKey("foo_pipeline_context_manager_test", InstanceType.PROXY);
        ContextManager originalProxyContext = PipelineContextManager.getProxyContext();
        try {
            ContextManager expected = mock(ContextManager.class);
            PipelineContextManager.putContext(proxyContextKey, expected);
            PipelineContextManager.putContext(fooContextKey, mock(ContextManager.class));
            assertThat(PipelineContextManager.getProxyContext(), sameInstance(expected));
            PipelineContextManager.removeContext(proxyContextKey);
            assertNull(PipelineContextManager.getProxyContext());
        } finally {
            PipelineContextManager.removeContext(proxyContextKey);
            if (null != originalProxyContext) {
                PipelineContextManager.putContext(proxyContextKey, originalProxyContext);
            }
        }
    }
    
    @Test
    void assertRemoveContext() {
        ContextManager contextManager = mock(ContextManager.class);
        PipelineContextManager.putContext(fooContextKey, contextManager);
        PipelineContextManager.removeContext(fooContextKey);
        assertNull(PipelineContextManager.getContext(fooContextKey));
        verify(contextManager, never()).close();
    }
}
