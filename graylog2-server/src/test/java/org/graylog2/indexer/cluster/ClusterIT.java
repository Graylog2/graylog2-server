/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.cluster.health.WatermarkSettings;
import org.graylog2.indexer.indices.HealthStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public abstract class ClusterIT extends ElasticsearchBaseTest {
    private static final String INDEX_NAME = "cluster_it_" + System.nanoTime();
    private static final String ALIAS_NAME = "cluster_it_alias_" + System.nanoTime();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IndexSetRegistry indexSetRegistry;

    private Cluster cluster;

    protected abstract ClusterAdapter clusterAdapter(Duration timeout);

    @Before
    public void setUp() throws Exception {
        client().createIndex(INDEX_NAME, 1, 0);
        client().addAliasMapping(INDEX_NAME, ALIAS_NAME);
        client().waitForGreenStatus(INDEX_NAME, ALIAS_NAME);

        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("cluster-it-%d").build()
        );
        final Duration requestTimeout = Duration.seconds(1L);
        cluster = new Cluster(indexSetRegistry, scheduler, requestTimeout, clusterAdapter(requestTimeout));
    }

    @Test
    public void getFileDescriptorStats() {
        final Set<NodeFileDescriptorStats> fileDescriptorStats = cluster.getFileDescriptorStats();
        assertThat(fileDescriptorStats).isNotEmpty();
    }

    @Test
    public void getDiskUsageStats() {
        final Set<NodeDiskUsageStats> diskUsageStats = cluster.getDiskUsageStats();
        assertThat(diskUsageStats).isNotEmpty();
    }

    @Test
    public void getClusterAllocationDiskSettings() throws Exception{
        final ClusterAllocationDiskSettings clusterAllocationDiskSettings = cluster.getClusterAllocationDiskSettings();

        //Default Elasticsearch settings in Elasticsearch 5.6
        assertThat(clusterAllocationDiskSettings.ThresholdEnabled()).isTrue();
        assertThat(clusterAllocationDiskSettings.watermarkSettings().type()).isEqualTo(WatermarkSettings.SettingsType.PERCENTAGE);
        assertThat(clusterAllocationDiskSettings.watermarkSettings().low()).isEqualTo(85D);
        assertThat(clusterAllocationDiskSettings.watermarkSettings().high()).isEqualTo(90D);
        assertThat(clusterAllocationDiskSettings.watermarkSettings().floodStage()).isNull();
    }

    @Test
    public void health() throws Exception {
        final String index = client().createRandomIndex("cluster_it_");
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{index});

        final Optional<HealthStatus> health = cluster.health();
        assertThat(health)
                .isPresent()
                .hasValueSatisfying(status -> assertThat(status).isEqualTo(HealthStatus.Green));

    }

    @Test
    public void health_returns_empty_with_missing_index() {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{"does_not_exist"});
        final Optional<HealthStatus> health = cluster.health();
        assertThat(health).isEmpty();
    }

    @Test
    public void deflectorHealth() {
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{ALIAS_NAME});
        final Optional<HealthStatus> deflectorHealth = cluster.deflectorHealth();
        assertThat(deflectorHealth)
                .isPresent()
                .hasValueSatisfying(status -> assertThat(status).isEqualTo(HealthStatus.Green));
    }

    @Test
    public void deflectorHealth_returns_empty_with_missing_index() {
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{"does_not_exist"});
        final Optional<HealthStatus> deflectorHealth = cluster.deflectorHealth();
        assertThat(deflectorHealth).isEmpty();
    }

    @Test
    public void nodeIdToName() throws Exception {
        final Cat nodesInfo = new Cat.NodesBuilder()
                .setParameter("h", "id,name")
                .setParameter("format", "json")
                .setParameter("full_id", "true")
                .build();
        final CatResult catResult = jestClient().execute(nodesInfo);
        final JsonNode result = catResult.getJsonObject().path("result");
        assertThat(result).isNotEmpty();

        final JsonNode node = result.path(0);
        final String nodeId = node.get("id").asText();
        final String expectedName = node.get("name").asText();

        final Optional<String> name = cluster.nodeIdToName(nodeId);
        assertThat(name)
                .isPresent()
                .contains(expectedName);
    }

    @Test
    public void nodeIdToName_returns_empty_with_invalid_node_id() {
        final Optional<String> name = cluster.nodeIdToName("invalid-node-id");
        assertThat(name).isEmpty();
    }

    @Test
    public void nodeIdToHostName() throws Exception {
        final Cat nodesInfo = new Cat.NodesBuilder()
                .setParameter("h", "id,host,ip")
                .setParameter("format", "json")
                .setParameter("full_id", "true")
                .build();
        final CatResult catResult = jestClient().execute(nodesInfo);
        final JsonNode result = catResult.getJsonObject().path("result");
        assertThat(result).isNotEmpty();

        final JsonNode node = result.path(0);
        final String nodeId = node.get("id").asText();
        // "host" only exists in Elasticsearch 2.x
        final String ip = node.path("ip").asText();
        final String expectedHostName = node.path("host").asText(ip);

        final Optional<String> hostName = cluster.nodeIdToHostName(nodeId);
        assertThat(hostName)
                .isPresent()
                .contains(expectedHostName);
    }

    @Test
    public void nodeIdToHostName_returns_empty_with_invalid_node_id() {
        final Optional<String> hostName = cluster.nodeIdToHostName("invalid-node-id");
        assertThat(hostName).isEmpty();
    }

    @Test
    public void isConnected() {
        assertThat(cluster.isConnected()).isTrue();
    }

    @Test
    public void isHealthy() throws Exception {
        final String index = client().createRandomIndex("cluster_it_");
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{index});
        when(indexSetRegistry.isUp()).thenReturn(true);

        assertThat(cluster.isHealthy()).isTrue();

    }

    @Test
    public void isHealthy_returns_false_with_missing_index() {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{"does-not-exist"});
        when(indexSetRegistry.isUp()).thenReturn(true);
        assertThat(cluster.isHealthy()).isFalse();
    }

    @Test
    public void isHealthy_returns_false_with_missing_write_aliases() throws Exception {
        client().createRandomIndex("cluster_it_");
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{INDEX_NAME});
        when(indexSetRegistry.isUp()).thenReturn(false);

        assertThat(cluster.isHealthy()).isFalse();
    }

    @Test
    public void isDeflectorHealthy() {
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{ALIAS_NAME});
        when(indexSetRegistry.isUp()).thenReturn(true);
        assertThat(cluster.isDeflectorHealthy()).isTrue();
    }

    @Test
    public void isDeflectorHealthy_returns_false_with_missing_aliases() {
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{"does-not-exist"});
        when(indexSetRegistry.isUp()).thenReturn(true);
        assertThat(cluster.isDeflectorHealthy()).isFalse();
    }

    @Test
    public void waitForConnectedAndDeflectorHealthy() throws Exception {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{INDEX_NAME});
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{ALIAS_NAME});
        when(indexSetRegistry.isUp()).thenReturn(true);

        cluster.waitForConnectedAndDeflectorHealthy();
    }
}
