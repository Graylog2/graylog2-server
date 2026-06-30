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
package org.graylog.datanode.opensearch;

import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OpensearchUpgradeActionTest {

    private static final String NODE_ID = "test-node-0000-0000-0000-000000000000";
    private static final Path DIST_PATH = Path.of("/opensearch");

    @Mock
    private DataNodeMetadataService metadataService;

    @Test
    void doesNotUpdateMetadataWhenNoCandidatesExist() {
        final OpensearchUpgradeAction action = upgradeAction("2.19.0");

        action.enableLatestVersion();

        verifyNoInteractions(metadataService);
    }

    @Test
    void doesNotUpdateMetadataWhenAllCandidatesAreLower() {
        final OpensearchDistribution distribution = new OpensearchDistribution(DIST_PATH, "2.19.0")
                .withOtherCandidates(List.of(new OpensearchDistribution(DIST_PATH, "2.18.0")));
        final OpensearchUpgradeAction action = upgradeAction(distribution);

        action.enableLatestVersion();

        verifyNoInteractions(metadataService);
    }

    @Test
    void storesHigherCandidateVersion() {
        final OpensearchDistribution distribution = new OpensearchDistribution(DIST_PATH, "2.19.0")
                .withOtherCandidates(List.of(new OpensearchDistribution(DIST_PATH, "2.19.5")));
        final OpensearchUpgradeAction action = upgradeAction(distribution);

        action.enableLatestVersion();

        verify(metadataService).setOpensearchVersions(NODE_ID, "2.19.5", null);
    }

    @Test
    void storesHighestCandidateVersionWhenMultipleExist() {
        final OpensearchDistribution distribution = new OpensearchDistribution(DIST_PATH, "2.19.0")
                .withOtherCandidates(List.of(
                        new OpensearchDistribution(DIST_PATH, "2.18.0"),
                        new OpensearchDistribution(DIST_PATH, "2.19.5"),
                        new OpensearchDistribution(DIST_PATH, "2.20.0")
                ));
        final OpensearchUpgradeAction action = upgradeAction(distribution);

        action.enableLatestVersion();

        verify(metadataService).setOpensearchVersions(NODE_ID, "2.20.0", null);
    }

    private OpensearchUpgradeAction upgradeAction(String version) {
        return upgradeAction(new OpensearchDistribution(DIST_PATH, version));
    }

    private OpensearchUpgradeAction upgradeAction(OpensearchDistribution distribution) {
        final DatanodeConfiguration config = new DatanodeConfiguration(distribution, null, 0, null);
        return new OpensearchUpgradeAction(config, metadataService, new SimpleNodeId(NODE_ID));
    }
}
