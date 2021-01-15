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
package org.graylog2.indexer.cluster;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.cluster.health.WatermarkSettings;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
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

    protected Cluster cluster;

    protected abstract ClusterAdapter clusterAdapter(Duration timeout);

    protected abstract String currentNodeId();
    protected abstract String currentNodeName();
    protected abstract String currentHostnameOrIp();

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
    public void health() {
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
    public void health_returns_green_with_no_indices() {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{});
        final Optional<HealthStatus> health = cluster.health();
        assertThat(health).contains(HealthStatus.Green);
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
    public void deflectorHealth_returns_green_with_empty_index() {
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{});
        final Optional<HealthStatus> deflectorHealth = cluster.deflectorHealth();
        assertThat(deflectorHealth).contains(HealthStatus.Green);
    }

    @Test
    public void nodeIdToName() {
        final Optional<String> name = cluster.nodeIdToName(currentNodeId());
        assertThat(name)
                .isPresent()
                .contains(currentNodeName());
    }

    @Test
    public void nodeIdToName_returns_empty_with_invalid_node_id() {
        final Optional<String> name = cluster.nodeIdToName("invalid-node-id");
        assertThat(name).isEmpty();
    }
    @Test
    public void nodeIdToHostName() {
        final Optional<String> hostName = cluster.nodeIdToHostName(currentNodeId());
        assertThat(hostName)
                .isPresent()
                .contains(currentHostnameOrIp());
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
    public void isHealthy() {
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
    public void isHealthy_returns_true_with_no_indices() {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{});
        when(indexSetRegistry.isUp()).thenReturn(true);
        assertThat(cluster.isHealthy()).isTrue();
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

    @Test
    public void retrievesClusterHealth() {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{INDEX_NAME});
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{ALIAS_NAME});
        when(indexSetRegistry.isUp()).thenReturn(true);

        final Optional<ClusterHealth> clusterHealth = cluster.clusterHealthStats();

        assertThat(clusterHealth).isNotEmpty();
    }

    @Test
    public void getDefaultClusterAllocationDiskSettings() {
        final ClusterAllocationDiskSettings clusterAllocationDiskSettings = cluster.getClusterAllocationDiskSettings();

        assertThat(clusterAllocationDiskSettings.ThresholdEnabled()).isTrue();
        assertThat(clusterAllocationDiskSettings.watermarkSettings().type()).isEqualTo(WatermarkSettings.SettingsType.PERCENTAGE);
        assertThat(clusterAllocationDiskSettings.watermarkSettings().low()).isEqualTo(85D);
        assertThat(clusterAllocationDiskSettings.watermarkSettings().high()).isEqualTo(90D);
        assertThat(clusterAllocationDiskSettings.watermarkSettings().floodStage()).isEqualTo(95D);
    }
}
