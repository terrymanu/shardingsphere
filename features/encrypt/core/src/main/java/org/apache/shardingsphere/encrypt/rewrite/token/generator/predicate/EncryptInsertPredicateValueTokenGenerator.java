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

package org.apache.shardingsphere.encrypt.rewrite.token.generator.predicate;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.shardingsphere.encrypt.rewrite.aware.EncryptConditionsAware;
import org.apache.shardingsphere.encrypt.rewrite.condition.EncryptCondition;
import org.apache.shardingsphere.encrypt.rule.EncryptRule;
import org.apache.shardingsphere.infra.annotation.HighFrequencyInvocation;
import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.context.statement.type.dml.InsertStatementContext;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.rewrite.sql.token.common.generator.CollectionSQLTokenGenerator;
import org.apache.shardingsphere.infra.rewrite.sql.token.common.generator.aware.ParametersAware;
import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;

import java.util.Collection;
import java.util.List;

/**
 * Insert predicate value token generator for encrypt.
 */
@HighFrequencyInvocation
@RequiredArgsConstructor
@Setter
public final class EncryptInsertPredicateValueTokenGenerator implements CollectionSQLTokenGenerator<InsertStatementContext>, ParametersAware, EncryptConditionsAware {
    
    private final EncryptRule rule;
    
    private final ShardingSphereDatabase database;
    
    private List<Object> parameters;
    
    private Collection<EncryptCondition> encryptConditions;
    
    @Override
    public boolean isGenerateSQLToken(final SQLStatementContext sqlStatementContext) {
        return sqlStatementContext instanceof InsertStatementContext && null != ((InsertStatementContext) sqlStatementContext).getInsertSelectContext()
                && !((InsertStatementContext) sqlStatementContext).getInsertSelectContext().getSelectStatementContext().getWhereSegments().isEmpty();
    }
    
    @Override
    public Collection<SQLToken> generateSQLTokens(final InsertStatementContext sqlStatementContext) {
        EncryptPredicateValueTokenGenerator generator = new EncryptPredicateValueTokenGenerator(rule, database);
        generator.setParameters(parameters);
        generator.setEncryptConditions(encryptConditions);
        return generator.generateSQLTokens(sqlStatementContext.getInsertSelectContext().getSelectStatementContext());
    }
}
