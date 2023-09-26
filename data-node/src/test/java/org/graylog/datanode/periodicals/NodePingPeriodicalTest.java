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

import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;

class NodePingPeriodicalTest {

    @Test
    void doRun() throws NodeNotFoundException {

        final SimpleNodeId nodeID = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
        final URI uri = URI.create("http://localhost:9200");
        final URI cluster = URI.create("http://localhost:9300");
        final NodeService nodeService = Mockito.mock(NodeService.class);

        final NodePingPeriodical task = new NodePingPeriodical(
                nodeService,
                nodeID,
                () -> uri,
                () -> cluster,
                () -> true
        );

        task.doRun();

        Mockito.verify(nodeService).markAsAlive(
                Mockito.eq(nodeID),
                Mockito.eq(true),
                Mockito.eq(uri),
                Mockito.eq(cluster));
    }


    @Test
    void doRunWithRegister() throws NodeNotFoundException {

        final SimpleNodeId nodeID = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
        final URI uri = URI.create("http://localhost:9200");
        final URI cluster = URI.create("http://localhost:9300");

        final NodeService nodeService = Mockito.mock(NodeService.class);

        Mockito.doThrow(new NodeNotFoundException("Node not found")).when(nodeService).markAsAlive(nodeID, true, uri);

        final NodePingPeriodical task = new NodePingPeriodical(
                nodeService,
                nodeID,
                () -> uri,
                () -> cluster,
                () -> true
        );

        task.doRun();

        Mockito.verify(nodeService).registerServer(
                Mockito.eq(nodeID.getNodeId()),
                Mockito.eq(true),
                Mockito.eq(uri),
                Mockito.eq(cluster),
                Mockito.any()
        );
    }
}
