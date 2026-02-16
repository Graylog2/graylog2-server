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
package org.graylog.plugins.datanode;

import com.github.zafarkhaja.semver.Version;
import org.assertj.core.api.Assertions;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog.plugins.datanode.dto.FlushResponse;
import org.graylog.plugins.datanode.dto.ShardReplication;
import org.graylog2.indexer.indices.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

public abstract class DatanodeUpgradeServiceAdapterIT {

    private DatanodeUpgradeServiceAdapter upgradeAdapter;

    @BeforeEach
    void setUp() {
        upgradeAdapter = createAdapter();
    }

    protected abstract DatanodeUpgradeServiceAdapter createAdapter();

    protected abstract Version indexerVersion();


    @Test
    void testUpgradeOperations() {
        Assertions.assertThat(upgradeAdapter.getClusterState())
                .satisfies(expectedState(HealthStatus.Green, ShardReplication.ALL))
                .satisfies(nodeDetails(indexerVersion()));

        Assertions.assertThat(upgradeAdapter.disableShardReplication())
                .satisfies(this::successfulFlush);

        Assertions.assertThat(upgradeAdapter.getClusterState())
                .satisfies(expectedState(HealthStatus.Green, ShardReplication.PRIMARIES));

        Assertions.assertThat(upgradeAdapter.enableShardReplication())
                .satisfies(this::successfulFlush);

        Assertions.assertThat(upgradeAdapter.getClusterState())
                .satisfies(expectedState(HealthStatus.Green, ShardReplication.ALL));
    }

    private Consumer<ClusterState> nodeDetails(Version version) {
        return state -> Assertions.assertThat(state.opensearchNodes())
                .hasSize(1)
                .allSatisfy(node -> {
                    Assertions.assertThat(node.name()).isEqualTo(state.managerNode().name());
                    Assertions.assertThat(node.roles()).contains("cluster_manager");
                    Assertions.assertThat(node.version()).isEqualTo(version.toString());
                });
    }

    private void successfulFlush(FlushResponse response) {
        Assertions.assertThat(response)
                .satisfies(flushResponse -> {
                    // if the cluster is new, it may have no indices and no shards
                    //Assertions.assertThat(flushResponse.total()).isEqualTo(1);
                    //Assertions.assertThat(flushResponse.successful()).isEqualTo(1);
                    Assertions.assertThat(flushResponse.failed()).isEqualTo(0);
                });
    }

    private Consumer<ClusterState> expectedState(HealthStatus status, ShardReplication shardReplication) {
        return clusterState -> {
            Assertions.assertThat(clusterState.status()).isEqualTo(status);
            Assertions.assertThat(clusterState.shardReplication()).isEqualTo(shardReplication);
        };
    }
}
