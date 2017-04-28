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
package org.graylog2;

import com.lordofthejars.nosqlunit.elasticsearch2.ElasticsearchRule;
import com.lordofthejars.nosqlunit.elasticsearch2.EmbeddedElasticsearch;
import io.searchbox.client.JestClient;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.graylog2.indexer.counts.JestClientRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import javax.inject.Inject;
import java.io.IOException;
import java.net.ServerSocket;

import static com.lordofthejars.nosqlunit.elasticsearch2.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.lordofthejars.nosqlunit.elasticsearch2.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractESTest {
    private static final Integer ES_HTTP_PORT = getFreePort();

    public static final TimeValue ES_TIMEOUT = TimeValue.timeValueSeconds(5L);

    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule()
            .settings(Settings.builder().put("http.port", ES_HTTP_PORT).build())
            .build();

    @Rule
    public ElasticsearchRule elasticsearchRule = newElasticsearchRule().defaultEmbeddedElasticsearch();

    @Rule
    public JestClientRule jestClientRule = JestClientRule.forEsHttpPort(ES_HTTP_PORT);

    @Inject
    private Client client;

    private JestClient jestClient;

    @Before
    public void setUp() throws Exception {
        this.jestClient = jestClientRule.getJestClient();
    }

    protected Client client() {
        return client;
    }

    protected JestClient jestClient() {
        return jestClient;
    }

    private static Integer getFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Unable to find free http port for embedded elasticsearch, aborting test: ", e);
        }
    }

    public void createIndex(String index) {
        createIndex(index, 1, 0);
    }

    public void createIndex(String index, int shards, int replicas) {
        createIndex(index, shards, replicas, Settings.EMPTY);
    }

    public void createIndex(String index, int shards, int replicas, Settings customSettings) {
        final CreateIndexResponse createIndexResponse = client().admin().indices().prepareCreate(index)
                .setSettings(Settings.builder()
                        .put("number_of_shards", shards)
                        .put("number_of_replicas", replicas)
                        .put(customSettings)
                        .build())
                .setTimeout(ES_TIMEOUT)
                .get();
        assertThat(createIndexResponse.isAcknowledged()).isTrue();
    }

    public void deleteIndex(String... indices) {
        final DeleteIndexResponse deleteIndexResponse = client().admin().indices().prepareDelete(indices)
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setTimeout(ES_TIMEOUT)
                .get();
        assertThat(deleteIndexResponse.isAcknowledged()).isTrue();
    }

    public void waitForGreenStatus(String... indices) {
        waitForStatus(ClusterHealthStatus.GREEN, indices);
    }

    public ClusterHealthStatus waitForStatus(ClusterHealthStatus status, String... indices) {
        final ClusterHealthResponse clusterHealthResponse = client().admin().cluster().prepareHealth(indices)
                .setWaitForStatus(status)
                .setTimeout(ES_TIMEOUT)
                .get();
        final ClusterHealthStatus clusterHealthStatus = clusterHealthResponse.getStatus();
        assertThat(clusterHealthStatus).isEqualTo(status);

        return clusterHealthStatus;
    }

}
