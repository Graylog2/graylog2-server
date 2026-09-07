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
package org.graylog.datanode.configuration;

import jakarta.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.filesystem.index.DataDirVerificationMarker;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParseResult;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParser;
import org.graylog.datanode.filesystem.index.RequiredOpensearchDistribution;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.graylog.datanode.filesystem.index.dto.NodeInformation;
import org.graylog.datanode.filesystem.index.indexreader.Lucene9ShardStatsParser;
import org.graylog.datanode.filesystem.index.indexreader.ShardStatsParserImpl;
import org.graylog.datanode.filesystem.index.statefile.Lucene9StateFileParser;
import org.graylog.datanode.filesystem.index.statefile.StateFile;
import org.graylog.datanode.filesystem.index.statefile.StateFileParserImpl;
import org.graylog2.cluster.nodes.DataNodeMetadata;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.cluster.nodes.OpensearchVersionsOverview;
import org.graylog2.plugin.system.NodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class OpensearchVersionSelectorImplTest {

    private static final String COMPAT = "2.19.5";
    private static final String CURRENT = "3.5.0";
    private static final String NODE_ID = "node-1";

    private final DataDirVerificationMarker marker = new DataDirVerificationMarker();

    /**
     * A genuinely new node: nothing recorded, no indices to preserve, so it may start on the newest distribution.
     */
    @Test
    void testEmptyDataDirTakesTheNewestDistribution(@TempDir Path dataDir) {
        Assertions.assertThat(select(dataDir, null, false).version()).isEqualTo(CURRENT);
    }

    /**
     * Data adopted from an existing cluster during an in-place migration has no recorded version. Moving it up a
     * generation rewrites index formats irreversibly, so it stays on the oldest distribution that can read it and the
     * administrator upgrades deliberately.
     */
    @Test
    void testAdoptedDataStaysOnTheOldestDistribution() throws URISyntaxException {
        Assertions.assertThat(select(fixture("opensearch2"), null, false).version()).isEqualTo(COMPAT);
    }

    /**
     * The recorded version wins even though a newer distribution is available.
     */
    @Test
    void testRecordedVersionWinsOverNewerDistribution() throws URISyntaxException {
        Assertions.assertThat(select(fixture("opensearch2"), COMPAT, false).version()).isEqualTo(COMPAT);
    }

    /**
     * The marker records that this major already opened the directory, so no scan is needed. The parser is a stub that
     * fails the test if it is consulted.
     */
    @Test
    void testVerifiedMarkerSkipsTheScan(@TempDir Path dataDir) throws URISyntaxException {
        marker.record(dataDir, CURRENT);

        final IndicesDirectoryParser neverCalled = new IndicesDirectoryParser(null, null, null, null) {
            @Override
            public org.graylog.datanode.filesystem.index.IndicesDirectoryParseResult parse(Path path) {
                throw new AssertionError("The data directory must not be scanned when the marker matches");
            }
        };

        final OpensearchDistribution selected = selectWith(dataDir, CURRENT, false, neverCalled);
        Assertions.assertThat(selected.version()).isEqualTo(CURRENT);
    }

    /**
     * Lucene 8 data can only be opened by the compatibility distribution. A recorded version from the current
     * generation contradicts that, which means something is wrong with the setup rather than something to silently
     * work around.
     */
    @Test
    void testRecordedVersionOutsideTheBoundFails() throws URISyntaxException {
        Assertions.assertThatThrownBy(() -> select(fixture("opensearch1"), CURRENT, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot open the data directory");
    }

    /**
     * In-place migration from an elasticsearch 7.x cluster. Its Lucene 8 data belongs to the opensearch 1.x
     * generation, so the compatibility distribution can adopt it and the current one cannot.
     */
    @Test
    void testElasticsearch7DataIsAdoptedByTheCompatDistribution() throws URISyntaxException {
        Assertions.assertThat(select(fixture("elasticsearch7"), null, false).version()).isEqualTo(COMPAT);
    }

    @Test
    void testRecordedCurrentVersionCannotOpenElasticsearch7Data() throws URISyntaxException {
        Assertions.assertThatThrownBy(() -> select(fixture("elasticsearch7"), CURRENT, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot open the data directory");
    }

    /**
     * In-place migration from a current-generation cluster: the data was written by 3.x and has no recorded version.
     * The compatibility distribution cannot read it, so "stay on the oldest" must not apply here.
     */
    @Test
    void testDataWrittenByCurrentGenerationIsNotHandedToTheCompatDistribution(@TempDir Path dataDir) {
        final OpensearchDistribution selected =
                selectWith(dataDir, null, false, parserReturning(dataWrittenBy(CURRENT)));

        Assertions.assertThat(selected.version()).isEqualTo(CURRENT);
    }

    /**
     * The same data with a recorded compatibility version is a contradiction — that version cannot open it.
     */
    @Test
    void testRecordedCompatVersionCannotOpenCurrentGenerationData(@TempDir Path dataDir) {
        Assertions.assertThatThrownBy(() -> selectWith(dataDir, COMPAT, false, parserReturning(dataWrittenBy(CURRENT))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot open the data directory");
    }

    /**
     * Data written by the previous generation can be opened by either, so the conservative choice still applies and
     * the administrator decides when to move up.
     */
    @Test
    void testDataWrittenByPreviousGenerationStaysOnTheOldestDistribution(@TempDir Path dataDir) {
        final OpensearchDistribution selected =
                selectWith(dataDir, null, false, parserReturning(dataWrittenBy(COMPAT)));

        Assertions.assertThat(selected.version()).isEqualTo(COMPAT);
    }

    /**
     * With the testing switch on, the recorded version is ignored — but the bound is not.
     */
    @Test
    void testAutoUpdateIgnoresTheRecordedVersion() throws URISyntaxException {
        Assertions.assertThat(select(fixture("opensearch2"), COMPAT, true).version()).isEqualTo(CURRENT);
    }

    @Test
    void testAutoUpdateStillRespectsTheBound() throws URISyntaxException {
        Assertions.assertThat(select(fixture("opensearch1"), null, true).version()).isEqualTo(COMPAT);
    }

    /**
     * An unreadable directory is not the selector's problem to report — the preflight check produces a usable
     * message. Here it only means we stay conservative.
     */
    @Test
    void testUnreadableDataDirStaysConservative(@TempDir Path parent) throws IOException {
        final Path missing = parent.resolve("does-not-exist");
        Assertions.assertThat(select(missing, null, false).version()).isEqualTo(COMPAT);
    }

    // ---------------------------------------------------------------------------------------------------------------

    private OpensearchDistribution select(Path dataDir, @Nullable String recordedVersion, boolean autoUpdate) {
        return selectWith(dataDir, recordedVersion, autoUpdate, realParser());
    }

    private OpensearchDistribution selectWith(Path dataDir, @Nullable String recordedVersion, boolean autoUpdate,
                                              IndicesDirectoryParser parser) {
        final OpensearchVersionSelectorImpl selector = new OpensearchVersionSelectorImpl(
                metadataService(recordedVersion), parser, marker, configuration(dataDir, autoUpdate), nodeId());
        return selector.select(List.of(
                new OpensearchDistribution(Path.of("/dist/opensearch-" + COMPAT), COMPAT),
                new OpensearchDistribution(Path.of("/dist/opensearch-" + CURRENT), CURRENT)));
    }

    private Configuration configuration(Path dataDir, boolean autoUpdate) {
        return new Configuration() {
            @Override
            public Path getOpensearchDataLocation() {
                return dataDir;
            }

            @Override
            public boolean isAutoUpdateOpensearch() {
                return autoUpdate;
            }
        };
    }

    private NodeId nodeId() {
        return new NodeId() {
            @Override
            public String getNodeId() {
                return NODE_ID;
            }
        };
    }

    private DataNodeMetadataService metadataService(@Nullable String recordedVersion) {
        return new DataNodeMetadataService() {
            @Override
            public void setOpensearchVersions(String nodeId, String currentVersion, @Nullable String latestAvailableVersion) {
            }

            @Override
            public void setLatestAvailableVersion(String nodeId, @Nullable String latestAvailableVersion) {
            }

            @Override
            public Optional<DataNodeMetadata> findByNodeId(String nodeId) {
                return Optional.ofNullable(recordedVersion).map(v -> new DataNodeMetadata(nodeId, v));
            }

            @Override
            public OpensearchVersionsOverview getVersionsOverview() {
                return OpensearchVersionsOverview.of(new ArrayList<>());
            }
        };
    }

    private IndicesDirectoryParser parserReturning(IndicesDirectoryParseResult result) {
        return new IndicesDirectoryParser(null, null, null, null) {
            @Override
            public IndicesDirectoryParseResult parse(Path path) {
                return result;
            }
        };
    }

    /**
     * A directory holding one node whose state file names {@code opensearchVersion} as its writer. Enough for the
     * selector, which only looks at the versions and at whether any node is present.
     */
    private IndicesDirectoryParseResult dataWrittenBy(String opensearchVersion) {
        final StateFile nodeState = new StateFile(Path.of("node-0.st"),
                Map.of("node_version", encodedVersionId(opensearchVersion)));
        final NodeInformation node = new NodeInformation(Path.of("nodes/0"), List.of(), nodeState);
        return new IndicesDirectoryParseResult(
                new IndexerDirectoryInformation(Path.of("data"), List.of(node)),
                RequiredOpensearchDistribution.CURRENT);
    }

    /**
     * Inverse of {@code OpensearchUtils#versionStringFromId}: major/minor/patch packed into an int, then masked the
     * way opensearch 2.x and 3.x store it.
     */
    private static int encodedVersionId(String opensearchVersion) {
        final com.github.zafarkhaja.semver.Version version =
                com.github.zafarkhaja.semver.Version.parse(opensearchVersion);
        final int raw = (int) (version.majorVersion() * 1_000_000
                + version.minorVersion() * 10_000
                + version.patchVersion() * 100);
        return raw ^ 0x08000000;
    }

    private IndicesDirectoryParser realParser() {
        return new IndicesDirectoryParser(new StateFileParserImpl(), new ShardStatsParserImpl(),
                new Lucene9StateFileParser(), new Lucene9ShardStatsParser());
    }

    private Path fixture(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/indices/" + name).toURI());
    }
}
