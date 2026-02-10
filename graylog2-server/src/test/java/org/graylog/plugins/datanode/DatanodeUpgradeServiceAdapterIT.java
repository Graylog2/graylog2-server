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

    private DatanodeUpgradeServiceAdapter migrationAdapter;

    @BeforeEach
    void setUp() {
        migrationAdapter = createAdapter();
    }

    protected abstract DatanodeUpgradeServiceAdapter createAdapter();

    protected abstract Version indexerVersion();


    @Test
    void testUpgradeOperations() {
        Assertions.assertThat(migrationAdapter.getClusterState())
                .satisfies(expectedState(HealthStatus.Green, ShardReplication.ALL))
                .satisfies(nodeDetails(indexerVersion()));

        Assertions.assertThat(migrationAdapter.disableShardReplication())
                .satisfies(this::successfulFlush);

        Assertions.assertThat(migrationAdapter.getClusterState())
                .satisfies(expectedState(HealthStatus.Green, ShardReplication.PRIMARIES));

        Assertions.assertThat(migrationAdapter.enableShardReplication())
                .satisfies(this::successfulFlush);

        Assertions.assertThat(migrationAdapter.getClusterState())
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
