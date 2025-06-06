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

package org.apache.shardingsphere.infra.util.datetime;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

/**
 * Date time formatter factory.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateTimeFormatterFactory {
    
    private static final DateTimeFormatter STANDARD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private static final DateTimeFormatter FULL_TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS");
    
    private static final DateTimeFormatter SHORT_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
    
    private static final DateTimeFormatter DOUBLE_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS");
    
    private static final DateTimeFormatter LONG_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private static final DateTimeFormatter FULL_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    
    /**
     * Get standard date time formatter.
     *
     * @return standard date time formatter
     */
    public static DateTimeFormatter getStandardFormatter() {
        return STANDARD;
    }
    
    /**
     * Get date formatter.
     *
     * @return date formatter
     */
    public static DateTimeFormatter getDateFormatter() {
        return DATE;
    }
    
    /**
     * Get full time formatter.
     *
     * @return full time formatter
     */
    public static DateTimeFormatter getFullTimeFormatter() {
        return FULL_TIME;
    }
    
    /**
     * Get time formatter.
     *
     * @return time formatter
     */
    public static DateTimeFormatter getTimeFormatter() {
        return TIME;
    }
    
    /**
     * Get short millis date time formatter.
     *
     * @return short millis date time formatter
     */
    public static DateTimeFormatter getShortMillisFormatter() {
        return SHORT_MILLIS;
    }
    
    /**
     * Get double millis date time formatter.
     *
     * @return double millis date time formatter
     */
    public static DateTimeFormatter getDoubleMillisFormatter() {
        return DOUBLE_MILLIS;
    }
    
    /**
     * Get long millis date time formatter.
     *
     * @return long millis date time formatter
     */
    public static DateTimeFormatter getLongMillisFormatter() {
        return LONG_MILLIS;
    }
    
    /**
     * Get full millis date time formatter.
     *
     * @return full millis date time formatter
     */
    public static DateTimeFormatter getFullMillisFormatter() {
        return FULL_MILLIS;
    }
}
