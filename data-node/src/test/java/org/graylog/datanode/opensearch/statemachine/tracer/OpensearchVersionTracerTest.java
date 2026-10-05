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
package org.graylog.datanode.opensearch.statemachine.tracer;

import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.filesystem.index.DataDirVerificationMarker;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpensearchVersionTracerTest {

    private static final String NODE_ID = "test-node-0000-0000-0000-000000000000";
    private static final Path DIST_PATH = Path.of("/opensearch");

    @TempDir
    private Path dataDir;

    private InMemoryDataNodeMetadataService metadataService;
    private DataDirVerificationMarker marker;

    @BeforeEach
    void setUp() {
        metadataService = new InMemoryDataNodeMetadataService();
        marker = new DataDirVerificationMarker();
    }

    @Test
    void doesNothingWhenDestinationIsNotAvailable() {
        final OpensearchVersionTracer tracer = tracerWithVersion("2.19.5");

        tracer.transition(OpensearchEvent.PROCESS_STARTED, OpensearchState.STARTING, OpensearchState.STARTING);
        tracer.transition(OpensearchEvent.PROCESS_STARTED, OpensearchState.STARTING, OpensearchState.FAILED);

        assertThat(metadataService.findByNodeId(NODE_ID)).isEmpty();
        assertThat(marker.verifiedMajorVersion(dataDir)).isEmpty();
    }

    @Test
    void doesNothingWhenSourceEqualsDestination() {
        final OpensearchVersionTracer tracer = tracerWithVersion("2.19.5");

        tracer.transition(OpensearchEvent.PROCESS_STARTED, OpensearchState.AVAILABLE, OpensearchState.AVAILABLE);

        assertThat(metadataService.findByNodeId(NODE_ID)).isEmpty();
        assertThat(marker.verifiedMajorVersion(dataDir)).isEmpty();
    }

    @Test
    void storesVersionWhenNoPreviousVersionExists() {
        final OpensearchVersionTracer tracer = tracerWithVersion("2.19.5");

        tracer.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);

        assertThat(metadataService.findByNodeId(NODE_ID))
                .hasValueSatisfying(m -> assertThat(m.currentOpensearchVersion()).isEqualTo("2.19.5"));
    }

    @Test
    void storesVersionWhenCurrentVersionIsNewer() {
        metadataService.setOpensearchVersions(NODE_ID, "2.18.0", null);
        final OpensearchVersionTracer tracer = tracerWithVersion("2.19.5");

        tracer.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);

        assertThat(metadataService.findByNodeId(NODE_ID))
                .hasValueSatisfying(m -> assertThat(m.currentOpensearchVersion()).isEqualTo("2.19.5"));
    }

    @Test
    void doesNotStoreVersionWhenCurrentVersionIsOlder() {
        metadataService.setOpensearchVersions(NODE_ID, "2.19.5", null);
        final OpensearchVersionTracer tracer = tracerWithVersion("2.18.0");

        tracer.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);

        assertThat(metadataService.findByNodeId(NODE_ID))
                .hasValueSatisfying(m -> assertThat(m.currentOpensearchVersion()).isEqualTo("2.19.5"));
    }

    @Test
    void doesNotStoreVersionWhenCurrentVersionIsSame() {
        metadataService.setOpensearchVersions(NODE_ID, "2.19.5", null);
        final OpensearchVersionTracer tracer = tracerWithVersion("2.19.5");

        tracer.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);

        assertThat(metadataService.findByNodeId(NODE_ID))
                .hasValueSatisfying(m -> assertThat(m.currentOpensearchVersion()).isEqualTo("2.19.5"));
    }

    /**
     * The node is already running its recorded version, so nothing about the current version changes — but a newly
     * shipped distribution still has to become visible, otherwise the upgrade page never offers it again.
     */
    @Test
    void refreshesLatestAvailableWhenRunningTheRecordedVersion() {
        metadataService.setOpensearchVersions(NODE_ID, "2.19.5", null);
        final OpensearchVersionTracer tracer = tracerWithVersion("2.19.5", "3.5.0");

        tracer.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);

        assertThat(metadataService.findByNodeId(NODE_ID)).hasValueSatisfying(m -> {
            assertThat(m.currentOpensearchVersion()).isEqualTo("2.19.5");
            assertThat(m.latestAvailableOpensearchVersion()).isEqualTo("3.5.0");
        });
    }

    @Test
    void clearsLatestAvailableWhenNothingNewerIsShipped() {
        metadataService.setOpensearchVersions(NODE_ID, "3.5.0", "3.5.0");
        final OpensearchVersionTracer tracer = tracerWithVersion("3.5.0", "2.19.5");

        tracer.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);

        assertThat(metadataService.findByNodeId(NODE_ID))
                .hasValueSatisfying(m -> assertThat(m.latestAvailableOpensearchVersion()).isNull());
    }

    /**
     * Opensearch reaching AVAILABLE is proof that this version opened the data directory, which is what lets the next
     * startup skip the directory scan.
     */
    @Test
    void recordsTheVerificationMarker() {
        final OpensearchVersionTracer tracer = tracerWithVersion("2.19.5");

        tracer.transition(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.STARTING, OpensearchState.AVAILABLE);

        assertThat(marker.verifiedMajorVersion(dataDir)).contains(2L);
        assertThat(marker.isVerifiedFor(dataDir, "2.19.5")).isTrue();
        assertThat(marker.isVerifiedFor(dataDir, "3.5.0")).isFalse();
    }

    private OpensearchVersionTracer tracerWithVersion(String version, String... otherCandidateVersions) {
        OpensearchDistribution distribution = new OpensearchDistribution(DIST_PATH, version);
        if (otherCandidateVersions.length > 0) {
            distribution = distribution.withOtherCandidates(List.of(otherCandidateVersions).stream()
                    .map(v -> new OpensearchDistribution(DIST_PATH.resolve(v), v))
                    .toList());
        }
        final DatanodeConfiguration config = new DatanodeConfiguration(
                distribution, DatanodeTestUtils.tempDirectories(dataDir), 0, null);
        return new OpensearchVersionTracer(config, metadataService, marker, new SimpleNodeId(NODE_ID));
    }
}
