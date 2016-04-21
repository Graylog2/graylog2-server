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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.nosqlunit.elasticsearch2.ElasticsearchRule;
import com.github.joschi.nosqlunit.elasticsearch2.EmbeddedElasticsearch;
import com.google.common.collect.ImmutableMap;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateResponse;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.index.IndexNotFoundException;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.nosqlunit.IndexCreatingLoadStrategyFactory;
import org.graylog2.indexer.searches.TimestampStats;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.joschi.nosqlunit.elasticsearch2.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.github.joschi.nosqlunit.elasticsearch2.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class IndicesTest {
    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule().build();

    private static final long ES_TIMEOUT = TimeUnit.SECONDS.toMillis(1L);
    private static final String INDEX_NAME = "graylog_0";
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
        this.elasticsearchRule.setLoadStrategyFactory(new IndexCreatingLoadStrategyFactory(CONFIG, Collections.singleton(INDEX_NAME)));
    }

    @Before
    public void setUp() throws Exception {
        indices = new Indices(client, CONFIG, new IndexMapping(), new Messages(client, CONFIG, new MetricRegistry()));
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

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testTimestampStatsOfIndex() throws Exception {
        TimestampStats stats = indices.timestampStatsOfIndex(INDEX_NAME);

        assertThat(stats.min()).isEqualTo(new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC));
    }

    @Test
    @UsingDataSet(locations = "IndicesTest-EmptyIndex.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testTimestampStatsOfIndexWithEmptyIndex() throws Exception {
        TimestampStats stats = indices.timestampStatsOfIndex(INDEX_NAME);

        assertThat(stats.min()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
    }

    @Test(expected = IndexNotFoundException.class)
    public void testTimestampStatsOfIndexWithNonExistingIndex() throws Exception {
        indices.timestampStatsOfIndex("does-not-exist");
    }

    @Test
    public void testCreateEnsuresIndexTemplateExists() throws Exception {
        final String templateName = CONFIG.getTemplateName();
        final IndicesAdminClient client = this.client.admin().indices();
        final GetIndexTemplatesRequest request = client.prepareGetTemplates(templateName).request();
        final GetIndexTemplatesResponse responseBefore = client.getTemplates(request).actionGet();

        assertThat(responseBefore.getIndexTemplates()).isEmpty();

        indices.create("index_template_test");

        final GetIndexTemplatesResponse responseAfter = client.getTemplates(request).actionGet();
        assertThat(responseAfter.getIndexTemplates()).hasSize(1);
        final IndexTemplateMetaData templateMetaData = responseAfter.getIndexTemplates().get(0);
        assertThat(templateMetaData.getName()).isEqualTo(templateName);
        assertThat(templateMetaData.getMappings().keysIt()).containsExactly(IndexMapping.TYPE_MESSAGE);

        final DeleteIndexTemplateRequest deleteRequest = client.prepareDeleteTemplate(templateName).request();
        final DeleteIndexTemplateResponse deleteResponse = client.deleteTemplate(deleteRequest).actionGet();
        assertThat(deleteResponse.isAcknowledged()).isTrue();

        indices.delete("index_template_test");
    }

    @Test
    public void testCreateOverwritesIndexTemplate() throws Exception {
        final ObjectMapper mapper = new ObjectMapperProvider().get();
        final String templateName = CONFIG.getTemplateName();
        final IndicesAdminClient client = this.client.admin().indices();

        final ImmutableMap<String, Object> beforeMapping = ImmutableMap.of(
            "_source", ImmutableMap.of("enabled", false),
            "properties", ImmutableMap.of("message",
                ImmutableMap.of(
                    "type", "binary",
                    "index", "not_analyzed")));
        assertThat(client.preparePutTemplate(templateName)
                .setTemplate(indices.allIndicesAlias())
                .addMapping(IndexMapping.TYPE_MESSAGE, beforeMapping)
                .get()
                .isAcknowledged())
            .isTrue();

        final GetIndexTemplatesResponse responseBefore = client.prepareGetTemplates(templateName).get();
        final List<IndexTemplateMetaData> beforeIndexTemplates = responseBefore.getIndexTemplates();
        assertThat(beforeIndexTemplates).hasSize(1);
        final ImmutableOpenMap<String, CompressedXContent> beforeMappings = beforeIndexTemplates.get(0).getMappings();
        final Map<String, Object> actualMapping = mapper.readValue(beforeMappings.get(IndexMapping.TYPE_MESSAGE).uncompressed(), new TypeReference<Map<String, Object>>() {});
        assertThat(actualMapping.get(IndexMapping.TYPE_MESSAGE)).isEqualTo(beforeMapping);

        indices.create("index_template_test");

        final GetIndexTemplatesResponse responseAfter = client.prepareGetTemplates(templateName).get();
        assertThat(responseAfter.getIndexTemplates()).hasSize(1);
        final IndexTemplateMetaData templateMetaData = responseAfter.getIndexTemplates().get(0);
        assertThat(templateMetaData.getName()).isEqualTo(templateName);
        assertThat(templateMetaData.getMappings().keysIt()).containsExactly(IndexMapping.TYPE_MESSAGE);

        final Map<String, Object> mapping = mapper.readValue(templateMetaData.getMappings().get(IndexMapping.TYPE_MESSAGE).uncompressed(), new TypeReference<Map<String, Object>>() {});
        final Map<String, Object> expectedTemplate = new IndexMapping().messageTemplate(indices.allIndicesAlias(), CONFIG.getAnalyzer());
        assertThat(mapping).isEqualTo(expectedTemplate.get("mappings"));

        final DeleteIndexTemplateRequest deleteRequest = client.prepareDeleteTemplate(templateName).request();
        final DeleteIndexTemplateResponse deleteResponse = client.deleteTemplate(deleteRequest).actionGet();
        assertThat(deleteResponse.isAcknowledged()).isTrue();

        indices.delete("index_template_test");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void indexCreationDateReturnsIndexCreationDateOfExistingIndexAsDateTime() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        indices.create("index_creation_date_test");

        final DateTime indexCreationDate = indices.indexCreationDate("index_creation_date_test");
        org.assertj.jodatime.api.Assertions.assertThat(indexCreationDate).isAfterOrEqualTo(now);

        indices.delete("index_creation_date_test");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void indexCreationDateReturnsNullForNonExistingIndex() {
        final DateTime indexCreationDate = indices.indexCreationDate("index_missing");
        assertThat(indexCreationDate).isNull();
    }
}
