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
package org.graylog2.plugin.cluster;

import org.graylog.testing.cluster.ClusterConfigServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MongoDBExtension.class, ClusterConfigServiceExtension.class})
class ClusterIdServiceTest {
    private static final ClusterId CLUSTER_ID = ClusterId.create("00000000-0000-0000-0000-000000000000");

    private ClusterIdService clusterIdService;
    private Runnable initClusterId;

    @BeforeEach
    void setUp(ClusterConfigService clusterConfigService) {
        this.clusterIdService = new ClusterIdService(clusterConfigService);
        this.initClusterId = () -> clusterConfigService.write(CLUSTER_ID);
    }

    @Test
    void getString() {
        initClusterId.run();

        assertThat(clusterIdService.getString()).isEqualTo(CLUSTER_ID.clusterId());
    }

    @Test
    void getStringWithMissingClusterId() {
        assertThatThrownBy(() -> clusterIdService.getString())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cluster ID doesn't exist");
    }

    @Test
    void getStringWithBlankStringValue() {
        final var clusterConfigService = mock(ClusterConfigService.class);
        final var service = new ClusterIdService(clusterConfigService);

        when(clusterConfigService.get(ClusterId.class)).thenReturn(ClusterId.create(" "));

        assertThatThrownBy(service::getString)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cluster ID is blank");
    }

    @Test
    void get() {
        initClusterId.run();

        assertThat(clusterIdService.get()).isEqualTo(CLUSTER_ID);
    }

    @Test
    void getWithMissingClusterId() {
        assertThatThrownBy(() -> clusterIdService.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cluster ID doesn't exist");
    }

    @Test
    void getWithBlankStringValue() {
        final var clusterConfigService = mock(ClusterConfigService.class);
        final var service = new ClusterIdService(clusterConfigService);

        when(clusterConfigService.get(ClusterId.class)).thenReturn(ClusterId.create(" "));

        assertThatThrownBy(service::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cluster ID is blank");
    }
}
