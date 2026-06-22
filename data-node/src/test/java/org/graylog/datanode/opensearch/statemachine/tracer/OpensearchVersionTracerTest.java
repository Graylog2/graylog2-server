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

import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpensearchVersionTracerTest {

    private static final String NODE_ID = "test-node-0000-0000-0000-000000000000";
    private static final Path DIST_PATH = Path.of("/opensearch");

    private InMemoryDataNodeMetadataService metadataService;

    @BeforeEach
    void setUp() {
        metadataService = new InMemoryDataNodeMetadataService();
    }

    @Test
    void doesNothingWhenDestinationIsNotAvailable() {
        final OpensearchVersionTracer tracer = tracerWithVersion("2.19.5");

        tracer.transition(OpensearchEvent.PROCESS_STARTED, OpensearchState.STARTING, OpensearchState.STARTING);
        tracer.transition(OpensearchEvent.PROCESS_STARTED, OpensearchState.STARTING, OpensearchState.FAILED);

        assertThat(metadataService.findByNodeId(NODE_ID)).isEmpty();
    }

    @Test
    void doesNothingWhenSourceEqualsDestination() {
        final OpensearchVersionTracer tracer = tracerWithVersion("2.19.5");

        tracer.transition(OpensearchEvent.PROCESS_STARTED, OpensearchState.AVAILABLE, OpensearchState.AVAILABLE);

        assertThat(metadataService.findByNodeId(NODE_ID)).isEmpty();
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

    private OpensearchVersionTracer tracerWithVersion(String version) {
        final DatanodeConfiguration config = new DatanodeConfiguration(
                new OpensearchDistribution(DIST_PATH, version), null, 0, null);
        return new OpensearchVersionTracer(config, metadataService, new SimpleNodeId(NODE_ID));
    }
}
