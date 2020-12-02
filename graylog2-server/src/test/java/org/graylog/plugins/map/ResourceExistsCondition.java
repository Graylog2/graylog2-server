/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.map;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used in conjunction with {@link ConditionalRunner} to disable tests if one or more resources doesn't exist.
 * <p>
 * Example:
 * <pre>{@code
 *    @literal @RunWith(ConditionalRunner.class)
 *    @literal @ResourceExistsCondition({"/file1.txt", "/file2.txt"})
 *     public class GeoIpResolverEngineTest {
 *        @literal @Test
 *        @literal @ResourceExistsCondition("/file3.txt")
 *         public void test() {
 *         }
 *     }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceExistsCondition {
    /** List of resources that must exist to run the tests. */
    String[] value();
}

