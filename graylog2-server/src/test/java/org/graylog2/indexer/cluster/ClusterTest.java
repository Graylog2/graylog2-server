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
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.searchbox.client.JestResult;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.aliases.AddAliasMapping;
import io.searchbox.indices.aliases.ModifyAliases;
import io.searchbox.indices.aliases.RemoveAliasMapping;
import org.graylog2.AbstractESTest;
import org.graylog2.indexer.IndexSetRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ClusterTest extends AbstractESTest {
    private static final String INDEX_NAME = "cluster_it_" + System.nanoTime();
    private static final String ALIAS_NAME = "cluster_it_alias_" + System.nanoTime();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IndexSetRegistry indexSetRegistry;

    private Cluster cluster;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Map<String, Map<String, Object>> settings = ImmutableMap.of("settings", ImmutableMap.of(
                "number_of_shards", 1,
                "number_of_replicas", 0));
        final CreateIndex createIndex = new CreateIndex.Builder(INDEX_NAME).settings(settings).refresh(true).build();
        final JestResult createIndexResponse = jestClient().execute(createIndex);
        assertThat(createIndexResponse.isSucceeded()).isTrue();

        final AddAliasMapping addAliasMapping = new AddAliasMapping.Builder(INDEX_NAME, ALIAS_NAME).build();
        final ModifyAliases modifyAliases = new ModifyAliases.Builder(addAliasMapping).refresh(true).build();
        final JestResult modifyAliasesResponse = jestClient().execute(modifyAliases);
        assertThat(modifyAliasesResponse.isSucceeded()).isTrue();

        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("cluster-it-%d").build()
        );
        cluster = new Cluster(jestClient(), indexSetRegistry, scheduler, Duration.seconds(1L));
    }

    @After
    public void tearDown() throws Exception {
        final RemoveAliasMapping removeAliasMapping = new RemoveAliasMapping.Builder(INDEX_NAME, ALIAS_NAME).build();
        final ModifyAliases modifyAliases = new ModifyAliases.Builder(removeAliasMapping).refresh(true).build();
        final JestResult modifyAliasesResponse = jestClient().execute(modifyAliases);
        assertThat(modifyAliasesResponse.isSucceeded()).isTrue();

        final DeleteIndex deleteIndex = new DeleteIndex.Builder(INDEX_NAME).refresh(true).build();
        final JestResult deleteIndexResponse = jestClient().execute(deleteIndex);
        assertThat(deleteIndexResponse.isSucceeded()).isTrue();
    }

    @Test
    public void getFileDescriptorStats() throws Exception {
        final Set<NodeFileDescriptorStats> fileDescriptorStats = cluster.getFileDescriptorStats();
        assertThat(fileDescriptorStats).isNotEmpty();
    }

    @Test
    public void health() throws Exception {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{INDEX_NAME});
        final Optional<JsonNode> health = cluster.health();
        assertThat(health)
                .isPresent()
                .hasValueSatisfying(json -> assertThat(json.path("status").asText()).isEqualTo("green"));
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
    public void nodeIdToName_returns_empty_with_invalid_node_id() throws Exception {
        final Optional<String> name = cluster.nodeIdToName("invalid-node-id");
        assertThat(name).isEmpty();
    }

    @Test
    public void nodeIdToHostName() throws Exception {
        final Cat nodesInfo = new Cat.NodesBuilder()
                .setParameter("h", "id,host")
                .setParameter("format", "json")
                .setParameter("full_id", "true")
                .build();
        final CatResult catResult = jestClient().execute(nodesInfo);
        final JsonNode result = catResult.getJsonObject().path("result");
        assertThat(result).isNotEmpty();

        final JsonNode node = result.path(0);
        final String nodeId = node.get("id").asText();
        final String expectedHostName = node.get("host").asText();

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
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{INDEX_NAME});
        when(indexSetRegistry.isUp()).thenReturn(true);
        assertThat(cluster.isHealthy()).isTrue();
    }

    @Test
    public void isHealthy_returns_false_with_missing_index() throws Exception {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{"does-not-exist"});
        when(indexSetRegistry.isUp()).thenReturn(true);
        assertThat(cluster.isHealthy()).isFalse();
    }

    @Test
    public void isHealthy_returns_false_with_missing_write_aliases() throws Exception {
        when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{INDEX_NAME});
        when(indexSetRegistry.isUp()).thenReturn(false);
        assertThat(cluster.isHealthy()).isFalse();
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