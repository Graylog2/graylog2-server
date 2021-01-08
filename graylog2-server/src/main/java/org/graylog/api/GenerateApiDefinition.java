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
package org.graylog.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.graylog2.shared.rest.documentation.generator.Generator;

import java.util.Collections;

public class GenerateApiDefinition {
    public static void main(String[] args) {
        System.out.println("Generating Swagger definition for API ...");
        final Generator generator = new Generator(ImmutableSet.of("org.graylog", "org.graylog2"), Collections.emptyMap(), "", new ObjectMapper());
        System.out.println(generator.generateOverview());
    }
}
