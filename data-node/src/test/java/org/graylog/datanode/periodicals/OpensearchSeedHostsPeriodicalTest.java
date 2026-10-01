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
package org.graylog.datanode.periodicals;

import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.configuration.OpensearchSeedHostsResolver;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.TestDataNodeNodeClusterService;
import org.graylog2.plugin.Tools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OpensearchSeedHostsPeriodicalTest {

    private final TestDataNodeNodeClusterService nodeService = new TestDataNodeNodeClusterService();
    private final OpensearchProcess process = mock(OpensearchProcess.class);

    @Test
    void testDeliversSeedHostsOfLaterRegisteredNodes(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final OpensearchSeedHostsPeriodical periodical = new OpensearchSeedHostsPeriodical(
                DatanodeTestUtils.datanodeConfiguration(Map.of(), tempDir), new OpensearchSeedHostsResolver(nodeService), process);
        assertThat(periodical.startOnThisNode()).isTrue();

        // no node registered yet, e.g. when all nodes start at the same time
        periodical.doRun();
        verify(process).updateSeedHosts(Set.of());

        registerNode("node1:9300");
        registerNode("node2:9300");
        periodical.doRun();
        verify(process).updateSeedHosts(Set.of("node1:9300", "node2:9300"));
    }

    @Test
    void testDisabledWithStaticSeedHosts(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final OpensearchSeedHostsPeriodical periodical = new OpensearchSeedHostsPeriodical(
                DatanodeTestUtils.datanodeConfiguration(Map.of("opensearch_discovery_seed_hosts", "node1:9300,node2:9300"), tempDir),
                new OpensearchSeedHostsResolver(nodeService), process);
        assertThat(periodical.startOnThisNode()).isFalse();
    }

    private void registerNode(String clusterAddress) {
        nodeService.registerServer(DataNodeDto.builder()
                .setId(Tools.generateServerId())
                .setHostname(clusterAddress.split(":")[0])
                .setClusterAddress(clusterAddress)
                .setDataNodeStatus(DataNodeStatus.STARTING)
                .build());
    }
}
