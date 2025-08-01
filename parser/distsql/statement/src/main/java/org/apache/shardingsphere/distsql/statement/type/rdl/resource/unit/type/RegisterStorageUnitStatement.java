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

package org.apache.shardingsphere.distsql.statement.type.rdl.resource.unit.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.distsql.segment.DataSourceSegment;
import org.apache.shardingsphere.distsql.statement.type.rdl.resource.unit.StorageUnitDefinitionStatement;

import java.util.Collection;

/**
 * Register storage unit statement.
 */
@RequiredArgsConstructor
@Getter
public final class RegisterStorageUnitStatement extends StorageUnitDefinitionStatement {
    
    private final boolean ifNotExists;
    
    private final Collection<DataSourceSegment> storageUnits;
    
    private final Collection<String> expectedPrivileges;
}
