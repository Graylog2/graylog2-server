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
package org.graylog.datanode.bootstrap.preflight;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParser;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.graylog.datanode.filesystem.index.indexreader.ShardStatsParserImpl;
import org.graylog.datanode.filesystem.index.statefile.StateFileParserImpl;
import org.graylog.security.certutil.InMemoryClusterConfigService;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class OpensearchDataDirCompatibilityCheckTest {

    @Test
    void testCompatibilityCheckSkipping() {
        final Path dataDir = Path.of("/opensearch/data");
        final String opensearchVersion = "2.19.0";

        final DatanodeDirectories directories = new DatanodeDirectories(dataDir, Path.of("/opensearch/logs"), null, Path.of("/opensearch/config"));
        final DatanodeConfiguration config = new DatanodeConfiguration(() -> new OpensearchDistribution(Path.of("/opensearch"), opensearchVersion), directories, 0, IndexerJwtAuthToken.disabled());

        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        clusterConfigService.write(new OpensearchDataDirCompatibilityCheck.CompatibilityCheck(dataDir, opensearchVersion));

        final IndicesDirectoryParser parser = new IndicesDirectoryParser(null, null) {
            @Override
            public IndexerDirectoryInformation parse(Path path) {
                throw new AssertionError("Should not be called");
            }
        };

        final OpensearchDataDirCompatibilityCheck check =
                new OpensearchDataDirCompatibilityCheck(config, parser, clusterConfigService);

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
    }

    @Test
    void testCompatibilityCheckInitialRun(@TempDir Path dataDir) {
        final String opensearchVersion = "2.19.0";

        final DatanodeDirectories directories = new DatanodeDirectories(dataDir, Path.of("/opensearch/logs"), null, Path.of("/opensearch/config"));
        final DatanodeConfiguration config = new DatanodeConfiguration(() -> new OpensearchDistribution(Path.of("/opensearch"), opensearchVersion), directories, 0, IndexerJwtAuthToken.disabled());

        // real parser: @TempDir has no nodes/ subdir so parse() returns empty immediately, sub-parsers are never invoked
        final IndicesDirectoryParser parser = new IndicesDirectoryParser(new StateFileParserImpl(), new ShardStatsParserImpl());

        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();

        final OpensearchDataDirCompatibilityCheck check =
                new OpensearchDataDirCompatibilityCheck(config, parser, clusterConfigService);

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
        Assertions.assertThat(clusterConfigService.get(OpensearchDataDirCompatibilityCheck.CompatibilityCheck.class))
                .isEqualTo(new OpensearchDataDirCompatibilityCheck.CompatibilityCheck(dataDir, opensearchVersion));
    }
}
