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

package org.apache.shardingsphere.test.it.sql.parser.internal.asserts.statement.rql.type;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.distsql.statement.type.rql.rule.database.ShowDatabaseRulesStatement;
import org.apache.shardingsphere.test.it.sql.parser.internal.asserts.SQLCaseAssertContext;
import org.apache.shardingsphere.test.it.sql.parser.internal.asserts.segment.database.DatabaseAssert;
import org.apache.shardingsphere.test.it.sql.parser.internal.cases.parser.jaxb.SQLParserTestCase;
import org.apache.shardingsphere.test.it.sql.parser.internal.cases.parser.jaxb.statement.DatabaseContainedTestCase;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Show rules statement assert.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShowRulesStatementAssert {
    
    /**
     * Assert show rules statement is correct with expected parser result.
     *
     * @param assertContext assert context
     * @param actual actual show rules statement
     * @param expected expected show rules statement test case
     */
    public static void assertIs(final SQLCaseAssertContext assertContext, final ShowDatabaseRulesStatement actual, final SQLParserTestCase expected) {
        assertThat("Expected value should be DatabaseContainedTestCase", expected, instanceOf(DatabaseContainedTestCase.class));
        assertIs(assertContext, actual, (DatabaseContainedTestCase) expected);
    }
    
    private static void assertIs(final SQLCaseAssertContext assertContext, final ShowDatabaseRulesStatement actual, final DatabaseContainedTestCase expected) {
        if (null == expected.getDatabase()) {
            assertNull(actual.getFromDatabase(), assertContext.getText("Actual database should not exist."));
        } else {
            DatabaseAssert.assertIs(assertContext, actual.getFromDatabase().getDatabase(), expected.getDatabase());
        }
    }
}
