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

import org.assertj.core.api.Assertions;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.cluster.nodes.DataNodeClusterService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.net.URI;
import java.util.Date;

@ExtendWith(MongoDBExtension.class)
class NodePingPeriodicalTest {

    private DataNodeClusterService nodeService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        nodeService = new DataNodeClusterService(mongodb.mongoConnection(), new org.graylog2.Configuration());
    }

    @Test
    void doRun() {

        final SimpleNodeId nodeID = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
        final URI uri = URI.create("http://localhost:9200");
        final String cluster = "localhost:9300";
        final String datanodeRestApi = "http://localhost:8999";
        @SuppressWarnings("unchecked")



        final NodePingPeriodical task = new NodePingPeriodical(
                nodeService,
                nodeID,
                new Configuration(),
                () -> uri,
                () -> cluster,
                () -> datanodeRestApi,
                () -> OpensearchState.AVAILABLE,
                Date::new
        );

        task.doRun();

        Assertions.assertThat(nodeService.allActive().values())
                .hasSize(1)
                .allSatisfy(nodeDto -> {
                    Assertions.assertThat(nodeDto.getTransportAddress()).isEqualTo("http://localhost:9200");
                    Assertions.assertThat(nodeDto.getClusterAddress()).isEqualTo("localhost:9300");
                    Assertions.assertThat(nodeDto.getDataNodeStatus()).isEqualTo(DataNodeStatus.AVAILABLE);
                    Assertions.assertThat(nodeDto.getNodeId()).isEqualTo("5ca1ab1e-0000-4000-a000-000000000000");
                    Assertions.assertThat(nodeDto.getLastSeen()).isNotNull();
                    Assertions.assertThat(nodeDto.getProvisioningInformation().certValidUntil()).isNotNull();
                });
    }

}
