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

package org.apache.shardingsphere.mode.manager.cluster.dispatch.handler.database.rule.type;

import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.mode.manager.cluster.dispatch.handler.database.rule.RuleItemConfigurationChangedHandler;
import org.apache.shardingsphere.mode.node.path.NodePath;
import org.apache.shardingsphere.mode.node.path.engine.searcher.NodePathPattern;
import org.apache.shardingsphere.mode.node.path.type.database.metadata.rule.DatabaseRuleItem;
import org.apache.shardingsphere.mode.node.path.type.database.metadata.rule.DatabaseRuleNodePath;

/**
 * Named rule item configuration changed handler.
 */
public final class NamedRuleItemConfigurationChangedHandler extends RuleItemConfigurationChangedHandler {
    
    public NamedRuleItemConfigurationChangedHandler(final ContextManager contextManager) {
        super(contextManager);
    }
    
    @Override
    public NodePath getSubscribedNodePath(final String databaseName) {
        return new DatabaseRuleNodePath(databaseName, NodePathPattern.IDENTIFIER, new DatabaseRuleItem(NodePathPattern.IDENTIFIER, "((?!(versions|active_version)$)[\\w-]+(?:[:.][\\w-]+)*)"));
    }
}
