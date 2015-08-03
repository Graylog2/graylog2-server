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
package org.graylog2.indexer.indices;

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule;
import com.lordofthejars.nosqlunit.elasticsearch.EmbeddedElasticsearch;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.nosqlunit.IndexCreatingLoadStrategyFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.lordofthejars.nosqlunit.elasticsearch.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class IndicesTest {
    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule().build();
    private static final long ES_TIMEOUT = TimeUnit.SECONDS.toMillis(1L);
    private static final String INDEX_NAME = "graylog";
    private static final ElasticsearchConfiguration CONFIG = new ElasticsearchConfiguration() {
        @Override
        public String getIndexPrefix() {
            return "graylog";
        }
    };

    @Rule
    public ElasticsearchRule elasticsearchRule;

    @Inject
    private Client client;
    private Indices indices;

    public IndicesTest() {
        this.elasticsearchRule = newElasticsearchRule().defaultEmbeddedElasticsearch();
        this.elasticsearchRule.setLoadStrategyFactory(
                new IndexCreatingLoadStrategyFactory(Collections.singleton(INDEX_NAME), CONFIG));
    }

    @Before
    public void setUp() throws Exception {
        indices = new Indices(client, CONFIG, new IndexMapping(client));
    }

    @Test
    public void testMove() throws Exception {

    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testDelete() throws Exception {
        final IndicesExistsRequest beforeRequest = client.admin().indices().prepareExists(INDEX_NAME).request();
        final IndicesExistsResponse beforeResponse = client.admin().indices().exists(beforeRequest).actionGet(ES_TIMEOUT);

        assertThat(beforeResponse.isExists()).isTrue();

        indices.delete(INDEX_NAME);

        final IndicesExistsRequest request = client.admin().indices().prepareExists(INDEX_NAME).request();
        final IndicesExistsResponse response = client.admin().indices().exists(request).actionGet(ES_TIMEOUT);

        assertThat(response.isExists()).isFalse();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testClose() throws Exception {
        final ClusterStateRequest beforeRequest = client.admin().cluster().prepareState().setIndices(INDEX_NAME).request();
        final ClusterStateResponse beforeResponse = client.admin().cluster().state(beforeRequest).actionGet(ES_TIMEOUT);
        assertThat(beforeResponse.getState().getMetaData().getConcreteAllOpenIndices()).containsExactly(INDEX_NAME);

        indices.close(INDEX_NAME);

        final ClusterStateRequest request = client.admin().cluster().prepareState().setIndices(INDEX_NAME).request();
        final ClusterStateResponse response = client.admin().cluster().state(request).actionGet(ES_TIMEOUT);
        assertThat(response.getState().getMetaData().getConcreteAllClosedIndices()).containsExactly(INDEX_NAME);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testAliasExists() throws Exception {
        assertThat(indices.aliasExists("graylog_alias")).isFalse();

        final IndicesAdminClient adminClient = client.admin().indices();
        final IndicesAliasesRequest request = adminClient.prepareAliases().addAlias(INDEX_NAME, "graylog_alias").request();
        final IndicesAliasesResponse response = adminClient.aliases(request).actionGet(ES_TIMEOUT);
        assertThat(response.isAcknowledged()).isTrue();
        assertThat(indices.aliasExists("graylog_alias")).isTrue();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testAliasTarget() throws Exception {
        assertThat(indices.aliasTarget("graylog_alias")).isNull();

        final IndicesAdminClient adminClient = client.admin().indices();
        final IndicesAliasesRequest request = adminClient.prepareAliases().addAlias(INDEX_NAME, "graylog_alias").request();
        final IndicesAliasesResponse response = adminClient.aliases(request).actionGet(ES_TIMEOUT);
        assertThat(response.isAcknowledged()).isTrue();
        assertThat(indices.aliasTarget("graylog_alias")).isEqualTo(INDEX_NAME);
    }
}