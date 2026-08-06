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

import com.github.zafarkhaja.semver.Version;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.filesystem.index.DataDirVerificationMarker;
import org.graylog.datanode.filesystem.index.IncompatibleIndexVersionException;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParseResult;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParser;
import org.graylog.datanode.filesystem.index.OpensearchDataDirCompatibilityService;
import org.graylog.datanode.filesystem.index.OpensearchUtils;
import org.graylog.datanode.filesystem.index.indexreader.Lucene9ShardStatsParser;
import org.graylog.datanode.filesystem.index.indexreader.ShardStatsParserImpl;
import org.graylog.datanode.filesystem.index.statefile.Lucene9StateFileParser;
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

    private final DataDirVerificationMarker marker = new DataDirVerificationMarker();

    @Test
    void testCompatibilityCheckSkipping(@TempDir Path tempDir) {
        marker.record(tempDir, OPENSEARCH_VERSION);

        final OpensearchDataDirCompatibilityCheck check =
                checkFor(tempDir, OPENSEARCH_VERSION, neverCalledParser("Should not be called"));

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
    }

    /**
     * The marker is written once opensearch actually reaches a running state, not by the check itself — see
     * {@code OpensearchVersionTracer}. Passing the check is only a prediction.
     */
    @Test
    void testCompatibilityCheckDoesNotWriteTheMarker(@TempDir Path tempDir) {
        final OpensearchDataDirCompatibilityCheck check = checkFor(tempDir, OPENSEARCH_VERSION, realParser());

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
        Assertions.assertThat(marker.verifiedMajorVersion(tempDir)).isEmpty();
    }

    @Test
    void testCompatibilityCheckSkipsOnMinorVersionChange(@TempDir Path dataDir) {
        marker.record(dataDir, "2.18.0");

        final OpensearchDataDirCompatibilityCheck check = checkFor(dataDir, OPENSEARCH_VERSION,
                neverCalledParser("Should not be called on minor version change"));

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
    }

    @Test
    void testCompatibilityCheckRerunsForMajorVersionChange(@TempDir Path dataDir) {
        marker.record(dataDir, "2.19.0");
        final String nextMajorVersion = "3.0.0";

        final OpensearchDataDirCompatibilityCheck check = checkFor(dataDir, nextMajorVersion, realParser());

        Assertions.assertThatCode(check::runCheck).doesNotThrowAnyException();
        // Still the old major: only a confirmed opensearch startup advances the marker
        Assertions.assertThat(marker.verifiedMajorVersion(dataDir)).contains(2L);
    }

    @Test
    void testCompatibilityCheckFailsForNonExistentDirectory() {
        final Path nonExistentDir = Path.of("/nonexistent/opensearch/data");

        final OpensearchDataDirCompatibilityCheck check = checkFor(nonExistentDir, OPENSEARCH_VERSION, realParser());

        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(PreflightCheckException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void testCompatibilityCheckFailsForIncompatibleIndexVersion(@TempDir Path dataDir) {
        final IndicesDirectoryParser parser = new IndicesDirectoryParser(null, null, null, null) {
            @Override
            public IndicesDirectoryParseResult parse(Path path) {
                throw new IncompatibleIndexVersionException("Index data version is not compatible");
            }
        };

        final OpensearchDataDirCompatibilityCheck check = checkFor(dataDir, OPENSEARCH_VERSION, parser);

        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(PreflightCheckException.class)
                .hasMessageContaining("is not compatible with current version " + OPENSEARCH_VERSION)
                .hasMessageContaining("Index data version is not compatible");
    }

    @Test
    void testIsCompatible() {
        // same major
        Assertions.assertThat(isCompatible("2.19.0", "2.19.0")).isTrue();
        Assertions.assertThat(isCompatible("2.19.0", "2.5.0")).isTrue();
        Assertions.assertThat(isCompatible("3.5.0", "3.0.0")).isTrue();

        // adjacent majors
        Assertions.assertThat(isCompatible("2.19.0", "3.5.0")).isTrue();
        Assertions.assertThat(isCompatible("3.5.0", "2.19.0")).isTrue();

        // two majors apart
        Assertions.assertThat(isCompatible("1.0.0", "3.5.0")).isFalse();
        Assertions.assertThat(isCompatible("3.5.0", "1.0.0")).isFalse();
        Assertions.assertThat(isCompatible("4.0.0", "2.0.0")).isFalse();
    }

    private static boolean isCompatible(String current, String node) {
        return OpensearchUtils.isCompatible(Version.parse(current), Version.parse(node));
    }

    /**
     * A parser that fails the test if the check reaches it at all, for the cases where the marker file has to
     * short-circuit the whole scan.
     */
    private static IndicesDirectoryParser neverCalledParser(String message) {
        return new IndicesDirectoryParser(null, null, null, null) {
            @Override
            public IndicesDirectoryParseResult parse(Path path) {
                throw new AssertionError(message);
            }
        };
    }

    private OpensearchDataDirCompatibilityCheck checkFor(Path dataDir, String opensearchVersion, IndicesDirectoryParser parser) {
        final DatanodeConfiguration configuration = configFor(dataDir, opensearchVersion);
        return new OpensearchDataDirCompatibilityCheck(configuration,
                new OpensearchDataDirCompatibilityService(configuration, parser), marker);
    }

    private DatanodeConfiguration configFor(Path dataDir, String opensearchVersion) {
        final DatanodeDirectories directories = DatanodeTestUtils.tempDirectories(dataDir);
        return new DatanodeConfiguration(new OpensearchDistribution(Path.of("/opensearch"), opensearchVersion), directories, 0, IndexerJwtAuthToken.disabled());
    }

    private IndicesDirectoryParser realParser() {
        return new IndicesDirectoryParser(new StateFileParserImpl(), new ShardStatsParserImpl(),
                new Lucene9StateFileParser(), new Lucene9ShardStatsParser());
    }

}
