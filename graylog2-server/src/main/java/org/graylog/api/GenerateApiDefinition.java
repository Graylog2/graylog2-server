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
import org.graylog2.shared.rest.documentation.generator.Generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
           bail("Syntax: " + GenerateApiDefinition.class.getSimpleName() + " <outfile> <package1> ... <packageN>");
        }
        final String targetName = args[0];
        Files.createDirectories(Paths.get(targetName));
        log("Generating Swagger definition for API ...");
        final Set<String> packageNames = Arrays.stream(args).skip(1).collect(Collectors.toSet());
        final Generator generator = new Generator(packageNames, Collections.emptyMap(), "/plugins", new ObjectMapper());

        final Map<String, Object> overview = generator.generateOverview();
        writeJsonToFile(targetName + "/api.json", overview);

        final List<Map<String, Object>> apis = retrieveApis(overview);

        apis.forEach(api -> {
            final String path = pathFromApi(api);
            try {
                log("Writing " + path);
                final Map<String, Object> apiResponse = generator.generateForRoute(path, "/");
                writeJsonToFile(targetName + path + ".json", apiResponse);
            } catch (IOException e) {
                log("Unable to write " + targetName + path + ":" + e);
            }
        });
        log("Done.");
    }

    private static String pathFromApi(Map<String, Object> api) {
        return (String)api.get("path");
    }
    private static List<Map<String, Object>> retrieveApis(Map<String, Object> overview) {
        return overview.containsKey("apis")
                ? (List<Map<String, Object>>)overview.get("apis")
                : Collections.emptyList();
    }

    private static void writeJsonToFile(String filename, Object content) throws IOException {
        final File file = new File(filename);
        file.getParentFile().mkdirs();
        file.createNewFile();

        final FileOutputStream outputStream = new FileOutputStream(file);

        objectMapper.writeValue(outputStream, content);
    }
}
