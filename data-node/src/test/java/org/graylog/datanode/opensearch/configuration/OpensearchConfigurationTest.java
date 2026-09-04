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
package org.graylog.datanode.opensearch.configuration;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

class OpensearchConfigurationTest {

    @Test
    void testDeduplicatedOpensearchRoles(@TempDir Path tempDir) {
        final DatanodeDirectories datanodeDirectories = new DatanodeDirectories(tempDir, tempDir, tempDir, tempDir);

        final DatanodeConfigurationPart cp1 = DatanodeConfigurationPart.builder().addNodeRole("search").build();
        final DatanodeConfigurationPart cp2 = DatanodeConfigurationPart.builder().addNodeRole("search").build();
        final DatanodeConfigurationPart cp3 = DatanodeConfigurationPart.builder().nodeRoles(List.of("cluster_manager", "data", "ingest", "remote_cluster_client")).build();

        final OpensearchConfiguration configuration = new OpensearchConfiguration(new OpensearchDistribution(tempDir, "2.15.0"), datanodeDirectories, datanodeDirectories.createUniqueOpensearchProcessConfigurationDir(), "localhost", 9200, List.of(cp1, cp2, cp3));

        Assertions.assertThat(configuration.opensearchRoles())
                .hasSize(5)
                .doesNotHaveDuplicates();
    }
}
