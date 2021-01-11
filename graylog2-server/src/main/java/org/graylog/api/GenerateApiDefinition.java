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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.rest.documentation.generator.Generator;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateApiDefinition {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static void log(String s) {
        System.out.println(s);
    }

    private static void bail(String s) {
        log(s);
        System.exit(-1);
    }

    public static void main(String[] args) throws JsonProcessingException {
        if (args.length < 2) {
           bail("Syntax: " + GenerateApiDefinition.class.getSimpleName() + " <outfile> <package1> ... <packageN>");
        }
        log("Generating Swagger definition for API ...");
        final String targetName = args[0];
        final Set<String> packageNames = Arrays.stream(args).skip(1).collect(Collectors.toSet());
        final Generator generator = new Generator(packageNames, Collections.emptyMap(), "", new ObjectMapper());

        log(objectMapper.writeValueAsString(generator.generateOverview()));
    }
}
