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
package org.graylog.datanode.docs;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.docs.printers.CsvDocsPrinter;
import org.graylog2.configuration.Documentation;
import org.graylog2.configuration.DocumentationSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class GenerateConfigDocumentationTest {

    private ConfigurationDocsGenerator generator;

    @BeforeEach
    void setUp() {
        this.generator = new ConfigurationDocsGenerator();
    }

    @Test
    void testCsv(@TempDir Path tmpPath) throws IOException {
        final Path file = tmpPath.resolve("my-documentation.csv");
        final DocumentationFormat format = new DocumentationFormat("csv", file.toFile().getAbsolutePath());
        generator.generateDocumentation(format, () -> List.of(new DummyConfiguration()));

        final CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.EXCEL)
                .setHeader(CsvDocsPrinter.HEADERS)
                .setSkipHeaderRecord(true)
                .build();

        final CSVParser parser = new CSVParser(new FileReader(file.toFile(), StandardCharsets.UTF_8), csvFormat);
        final List<CSVRecord> lines = parser.getRecords();
        Assertions.assertThat(lines)
                .hasSize(3) // one entry only, the invisible should not be present in the output
                .anySatisfy(line -> {
                    Assertions.assertThat(line.get(CsvDocsPrinter.HEADER_DEFAULT_VALUE)).isEqualTo("data/node-id");
                    Assertions.assertThat(line.get(CsvDocsPrinter.HEADER_PARAMETER)).isEqualTo("node_id_file");
                    Assertions.assertThat(line.get(CsvDocsPrinter.HEADER_TYPE)).isEqualTo("String");
                    Assertions.assertThat(line.get(CsvDocsPrinter.HEADER_REQUIRED)).isEqualTo("no");
                    Assertions.assertThat(line.get(CsvDocsPrinter.HEADER_DESCRIPTION)).contains("The auto-generated node ID will be stored in this file and read after restarts");
                });

        // Assert order of the lines. Required password_secret without default value has to go first. Then the rest follows
        // order of the properties in the class.
        Assertions.assertThat(lines.get(0).get(CsvDocsPrinter.HEADER_PARAMETER)).isEqualTo("password_secret");
        Assertions.assertThat(lines.get(1).get(CsvDocsPrinter.HEADER_PARAMETER)).isEqualTo("node_id_file");
        Assertions.assertThat(lines.get(2).get(CsvDocsPrinter.HEADER_PARAMETER)).isEqualTo("indexer_jwt_auth_token_caching_duration");
    }

    @Test
    void testConfFile(@TempDir Path tmpPath) throws IOException {
        final Path file = tmpPath.resolve("config-example.conf");
        final DocumentationFormat format = new DocumentationFormat("conf", file.toFile().getAbsolutePath());
        generator.generateDocumentation(format, () -> List.of(new DummyConfiguration()));
        final String content = Files.readString(file);
        Assertions.assertThat(content).contains("The auto-generated node ID will be stored in this file and read after restarts");
        Assertions.assertThat(content).contains("#node_id_file = data/node-id");
    }

    @DocumentationSection(heading = "my-test-config", description = "this is how you configure your app")
    private static class DummyConfiguration {
        @Documentation("""
                The auto-generated node ID will be stored in this file and read after restarts. It is a good idea
                to use an absolute file path here if you are starting Graylog DataNode from init scripts or similar.
                """)
        @Parameter(value = "node_id_file", validators = Configuration.NodeIdFileValidator.class)
        private String nodeIdFile = "data/node-id";

        @Documentation(visible = false)
        @Parameter(value = "timeout_sec", validators = PositiveIntegerValidator.class)
        private Integer timeoutSec;

        @Documentation("""
            You MUST set a secret to secure/pepper the stored user passwords here. Use at least 64 characters.
            Generate one by using for example: pwgen -N 1 -s 96
            ATTENTION: This value must be the same on all Graylog and Datanode nodes in the cluster.
            Changing this value after installation will render all user sessions and encrypted values
            in the database invalid. (e.g. encrypted access tokens)
            """)
        @Parameter(value = "password_secret", required = true, validators = StringNotBlankValidator.class)
        private String passwordSecret;

        @DocumentationSection(heading = "OpenSearch JWT token usage", description = "Communication between Graylog and OpenSearch is secured by JWT.")
        @Documentation("""
            This configuration defines interval between token regenerations.
            """)
        @Parameter(value = "indexer_jwt_auth_token_caching_duration")
        Duration indexerJwtAuthTokenCachingDuration = Duration.seconds(60);
    }
}
