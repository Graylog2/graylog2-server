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
import org.graylog2.ElasticsearchBase;
import org.graylog2.indexer.IndexSetRegistry;
import org.junit.After;
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

public class ClusterIT extends ElasticsearchBase {
    private static final String INDEX_NAME = "cluster_it_" + System.nanoTime();
    private static final String ALIAS_NAME = "cluster_it_alias_" + System.nanoTime();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IndexSetRegistry indexSetRegistry;

    private Cluster cluster;

    @Before
    public void setUp() throws Exception {
        createIndex(INDEX_NAME, 1, 0);
        addAliasMapping(INDEX_NAME, ALIAS_NAME);
        waitForGreenStatus(INDEX_NAME, ALIAS_NAME);
        
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("cluster-it-%d").build()
        );
        cluster = new Cluster(client(), indexSetRegistry, scheduler, Duration.seconds(1L));
    }

    @After
    public void tearDown() throws Exception {
        removeAliasMapping(INDEX_NAME, ALIAS_NAME);
        deleteIndex(INDEX_NAME);
    }

    @Test
    public void getFileDescriptorStats() throws Exception {
        final Set<NodeFileDescriptorStats> fileDescriptorStats = cluster.getFileDescriptorStats();
        assertThat(fileDescriptorStats).isNotEmpty();
    }

    @Test
    public void health() throws Exception {
        final String index = createRandomIndex("cluster_it_");
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{index});

        try {
            final Optional<JsonNode> health = cluster.health();
            assertThat(health)
                    .isPresent()
                    .hasValueSatisfying(json -> assertThat(json.path("status").asText()).isEqualTo("green"));
        } finally {
            deleteIndex(index);
        }
    }

    @Test
    public void health_returns_empty_with_missing_index() throws Exception {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{"does_not_exist"});
        final Optional<JsonNode> health = cluster.health();
        assertThat(health).isEmpty();
    }

    @Test
    public void deflectorHealth() throws Exception {
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{ALIAS_NAME});
        final Optional<JsonNode> deflectorHealth = cluster.deflectorHealth();
        assertThat(deflectorHealth)
                .isPresent()
                .hasValueSatisfying(json -> assertThat(json.path("status").asText()).isEqualTo("green"));
    }

    @Test
    public void deflectorHealth_returns_empty_with_missing_index() throws Exception {
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{"does_not_exist"});
        final Optional<JsonNode> deflectorHealth = cluster.deflectorHealth();
        assertThat(deflectorHealth).isEmpty();
    }

    @Test
    public void nodeIdToName() throws Exception {
        final Cat nodesInfo = new Cat.NodesBuilder()
                .setParameter("h", "id,name")
                .setParameter("format", "json")
                .setParameter("full_id", "true")
                .build();
        final CatResult catResult = client().execute(nodesInfo);
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
    public void nodeIdToName_returns_empty_with_invalid_node_id() throws Exception {
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
        final CatResult catResult = client().execute(nodesInfo);
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
    public void nodeIdToHostName_returns_empty_with_invalid_node_id() throws Exception {
        final Optional<String> hostName = cluster.nodeIdToHostName("invalid-node-id");
        assertThat(hostName).isEmpty();
    }

    @Test
    public void isConnected() throws Exception {
        assertThat(cluster.isConnected()).isTrue();
    }

    @Test
    public void isHealthy() throws Exception {
        final String index = createRandomIndex("cluster_it_");
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{index});
        when(indexSetRegistry.isUp()).thenReturn(true);

        try {
            assertThat(cluster.isHealthy()).isTrue();
        } finally {
            deleteIndex(index);
        }
    }

    @Test
    public void isHealthy_returns_false_with_missing_index() throws Exception {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{"does-not-exist"});
        when(indexSetRegistry.isUp()).thenReturn(true);
        assertThat(cluster.isHealthy()).isFalse();
    }

    @Test
    public void isHealthy_returns_false_with_missing_write_aliases() throws Exception {
        final String index = createRandomIndex("cluster_it_");
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{INDEX_NAME});
        when(indexSetRegistry.isUp()).thenReturn(false);

        try {
            assertThat(cluster.isHealthy()).isFalse();
        } finally {
            deleteIndex(index);
        }
    }

    @Test
    public void isDeflectorHealthy() throws Exception {
        when(indexSetRegistry.getWriteIndexAliases()).thenReturn(new String[]{ALIAS_NAME});
        when(indexSetRegistry.isUp()).thenReturn(true);
        assertThat(cluster.isDeflectorHealthy()).isTrue();
    }

    @Test
    public void isDeflectorHealthy_returns_false_with_missing_aliases() throws Exception {
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