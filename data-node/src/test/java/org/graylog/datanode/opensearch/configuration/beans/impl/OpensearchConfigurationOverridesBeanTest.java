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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

class OpensearchConfigurationOverridesBeanTest {

    @Test
    void testPassThroughConfigurationPart(@TempDir Path tempDir) {
        final DatanodeDirectories datanodeDirectories = new DatanodeDirectories(tempDir, tempDir, tempDir, tempDir);


        final Path opensearchOverridesFile = tempDir.resolve("opensearch.overrides");
        writePropertiesFile(opensearchOverridesFile,
                """
                        cluster.routing.allocation.disk.watermark.low: 98%
                        cluster.max_shards_per_node: 500
                        """);


        final OpensearchConfigurationOverridesBean cb = new OpensearchConfigurationOverridesBean(datanodeDirectories, opensearchOverridesFile, Collections::emptyMap);
        final DatanodeConfigurationPart configurationPart = cb.buildConfigurationPart(emptyBuildParams());

        Assertions.assertThat(configurationPart.properties())
                .hasSize(2)
                .containsEntry("cluster.routing.allocation.disk.watermark.low", "98%")
                .containsEntry("cluster.max_shards_per_node", "500");

    }

    @Test
    void testLegacyEnvProperties(@TempDir Path tempDir) {
        final DatanodeDirectories datanodeDirectories = new DatanodeDirectories(tempDir, tempDir, tempDir, tempDir);


        final Path opensearchOverridesFile = tempDir.resolve("opensearch.overrides");
        writePropertiesFile(opensearchOverridesFile,
                """
                        cluster.routing.allocation.disk.watermark.low=98%
                        """);


        final Supplier<Map<String, String>> env = () -> Map.of("opensearch.cluster.max_shards_per_node", "500");

        final OpensearchConfigurationOverridesBean cb = new OpensearchConfigurationOverridesBean(datanodeDirectories, opensearchOverridesFile, env);
        final DatanodeConfigurationPart configurationPart = cb.buildConfigurationPart(emptyBuildParams());

        Assertions.assertThat(configurationPart.properties())
                .hasSize(2)
                .containsEntry("cluster.routing.allocation.disk.watermark.low", "98%")
                .containsEntry("cluster.max_shards_per_node", "500");

        Assertions.assertThat(configurationPart.warnings())
                .hasSize(1)
                .anySatisfy(warning -> Assertions.assertThat(warning).contains("Detected pass-through opensearch property cluster.max_shards_per_node"));
    }


    private void writePropertiesFile(Path file, String passthroughConfig) {
        try {
            Files.writeString(file, passthroughConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OpensearchConfigurationParams emptyBuildParams() {
        return new OpensearchConfigurationParams(Collections.emptyList(), Collections.emptyMap());
    }
}
