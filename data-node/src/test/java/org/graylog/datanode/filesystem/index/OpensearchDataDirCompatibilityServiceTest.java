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
package org.graylog.datanode.filesystem.index;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.filesystem.index.indexreader.ShardStatsParserImpl;
import org.graylog.datanode.filesystem.index.statefile.StateFileParserImpl;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Path;

class OpensearchDataDirCompatibilityServiceTest {

    @Test
    void testCompatibleDataDir() throws URISyntaxException {
        final OpensearchDataDirCompatibility compatibility = serviceFor(opensearch2Fixture(), "2.19.0").check();

        Assertions.assertThat(compatibility.isCompatible()).isTrue();
        Assertions.assertThat(compatibility.errors()).isEmpty();
        Assertions.assertThat(compatibility.warnings()).isEmpty();
        Assertions.assertThat(compatibility.indicesCount()).isEqualTo(6);
    }

    /**
     * The node is two major versions ahead of the data, so both the node version and every single index are reported.
     * The per-index errors are the part that the REST endpoint used to miss, which let the migration state machine
     * pass a data directory that the datanode itself would then refuse to start on.
     */
    @Test
    void testIncompatibleDataDirReportsNodeAndIndexErrors() throws URISyntaxException {
        final OpensearchDataDirCompatibility compatibility = serviceFor(opensearch2Fixture(), "4.0.0").check();

        Assertions.assertThat(compatibility.isCompatible()).isFalse();
        Assertions.assertThat(compatibility.errors())
                .hasSize(7)
                .anySatisfy(error -> Assertions.assertThat(error)
                        .isEqualTo("Current version 4.0.0 of Opensearch is not compatible with index version 2.10.0"))
                .anySatisfy(error -> Assertions.assertThat(error)
                        .isEqualTo("Current version 4.0.0 is not compatible with index version 2.10.0 of index graylog_0"));
    }

    @Test
    void testEmptyDataDirProducesWarning(@TempDir Path emptyDir) {
        final OpensearchDataDirCompatibility compatibility = serviceFor(emptyDir, "2.19.0").check();

        Assertions.assertThat(compatibility.isCompatible()).isTrue();
        Assertions.assertThat(compatibility.warnings())
                .singleElement()
                .satisfies(warning -> Assertions.assertThat(warning).contains("doesn't contain any indices"));
    }

    @Test
    void testUnreadableDataDirIsReportedAsError() {
        final OpensearchDataDirCompatibility compatibility =
                serviceFor(Path.of("/nonexistent/opensearch/data"), "2.19.0").check();

        Assertions.assertThat(compatibility.isCompatible()).isFalse();
        Assertions.assertThat(compatibility.errors())
                .singleElement()
                .satisfies(error -> Assertions.assertThat(error).contains("nonexistent"));
        Assertions.assertThat(compatibility.info().nodes()).isEmpty();
    }

    private Path opensearch2Fixture() throws URISyntaxException {
        return Path.of(getClass().getResource("/indices/opensearch2").toURI());
    }

    private OpensearchDataDirCompatibilityService serviceFor(Path dataDir, String opensearchVersion) {
        final DatanodeConfiguration configuration = new DatanodeConfiguration(
                new OpensearchDistribution(Path.of("/opensearch"), opensearchVersion),
                DatanodeTestUtils.tempDirectories(dataDir), 0, IndexerJwtAuthToken.disabled());
        return new OpensearchDataDirCompatibilityService(configuration,
                new IndicesDirectoryParser(new StateFileParserImpl(), new ShardStatsParserImpl()));
    }
}
