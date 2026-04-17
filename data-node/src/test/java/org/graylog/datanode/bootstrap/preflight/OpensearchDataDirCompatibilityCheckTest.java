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
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.filesystem.index.IncompatibleIndexVersionException;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParser;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.graylog.datanode.filesystem.index.indexreader.ShardStatsParserImpl;
import org.graylog.datanode.filesystem.index.statefile.StateFileParserImpl;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class OpensearchDataDirCompatibilityCheckTest {

    private static final String OPENSEARCH_VERSION = "2.19.0";

    @Test
    void testCompatibilityCheckSkipping(@TempDir Path tempDir) throws IOException {
        writeCheckFile(tempDir, OPENSEARCH_VERSION);

        final IndicesDirectoryParser parser = new IndicesDirectoryParser(null, null) {
            @Override
            public IndexerDirectoryInformation parse(Path path) {
                throw new AssertionError("Should not be called");
            }
        };

        final OpensearchDataDirCompatibilityCheck check =
                new OpensearchDataDirCompatibilityCheck(configFor(tempDir, OPENSEARCH_VERSION), parser);

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
    }

    @Test
    void testCompatibilityCheckInitialRun(@TempDir Path tempDir) throws IOException {
        final OpensearchDataDirCompatibilityCheck check =
                new OpensearchDataDirCompatibilityCheck(configFor(tempDir, OPENSEARCH_VERSION), realParser());

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
        Assertions.assertThat(readCheckFile(tempDir)).isEqualTo(OPENSEARCH_VERSION);
    }

    @Test
    void testCompatibilityCheckSkipsOnMinorVersionChange(@TempDir Path dataDir) throws IOException {
        writeCheckFile(dataDir, "2.18.0");

        final IndicesDirectoryParser parser = new IndicesDirectoryParser(null, null) {
            @Override
            public IndexerDirectoryInformation parse(Path path) {
                throw new AssertionError("Should not be called on minor version change");
            }
        };

        final OpensearchDataDirCompatibilityCheck check =
                new OpensearchDataDirCompatibilityCheck(configFor(dataDir, OPENSEARCH_VERSION), parser);

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
    }

    @Test
    void testCompatibilityCheckRerunsForMajorVersionChange(@TempDir Path dataDir) throws IOException {
        writeCheckFile(dataDir, "2.19.0");
        final String nextMajorVersion = "3.0.0";

        final OpensearchDataDirCompatibilityCheck check =
                new OpensearchDataDirCompatibilityCheck(configFor(dataDir, nextMajorVersion), realParser());

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
        Assertions.assertThat(readCheckFile(dataDir)).isEqualTo(nextMajorVersion);
    }

    @Test
    void testCompatibilityCheckFailsForNonExistentDirectory() {
        final Path nonExistentDir = Path.of("/nonexistent/opensearch/data");

        final OpensearchDataDirCompatibilityCheck check =
                new OpensearchDataDirCompatibilityCheck(configFor(nonExistentDir, OPENSEARCH_VERSION), realParser());

        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(PreflightCheckException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void testCompatibilityCheckFailsForIncompatibleIndexVersion(@TempDir Path dataDir) {
        final IndicesDirectoryParser parser = new IndicesDirectoryParser(null, null) {
            @Override
            public IndexerDirectoryInformation parse(Path path) {
                throw new IncompatibleIndexVersionException("Index data version is not compatible");
            }
        };

        final OpensearchDataDirCompatibilityCheck check =
                new OpensearchDataDirCompatibilityCheck(configFor(dataDir, OPENSEARCH_VERSION), parser);

        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(PreflightCheckException.class)
                .hasMessageContaining("is not compatible with current version " + OPENSEARCH_VERSION);
    }

    private DatanodeConfiguration configFor(Path dataDir, String opensearchVersion) {
        final DatanodeDirectories directories = DatanodeTestUtils.tempDirectories(dataDir);
        return new DatanodeConfiguration(() -> new OpensearchDistribution(Path.of("/opensearch"), opensearchVersion), directories, 0, IndexerJwtAuthToken.disabled());
    }

    private IndicesDirectoryParser realParser() {
        return new IndicesDirectoryParser(new StateFileParserImpl(), new ShardStatsParserImpl());
    }

    private void writeCheckFile(Path dataDir, String version) throws IOException {
        Files.writeString(dataDir.resolve(OpensearchDataDirCompatibilityCheck.COMPATIBILITY_CHECK_FILENAME), version, StandardCharsets.UTF_8);
    }

    private String readCheckFile(Path dataDir) throws IOException {
        return Files.readString(dataDir.resolve(OpensearchDataDirCompatibilityCheck.COMPATIBILITY_CHECK_FILENAME), StandardCharsets.UTF_8);
    }
}
