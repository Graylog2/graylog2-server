package org.graylog.storage.elasticsearch7.views.migrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetIndexResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutMappingRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.cluster.metadata.MappingMetadata;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.IndicesAdapterES7;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.cluster.ClusterStateApi;
import org.graylog.storage.elasticsearch7.stats.StatsApi;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.indices.IndexSettings;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.Message.FIELD_GL2_MESSAGE_ID;


public class V20200730000000_AddGl2MessageIdFieldAliasForEventsES7IT extends ElasticsearchBaseTest {

    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    private IndicesAdapterES7 indicesAdapter;
    private V20200730000000_AddGl2MessageIdFieldAliasForEventsES7 sut;

    @Before
    public void setUp() {
        indicesAdapter = createIndicesAdapter();
        sut = new V20200730000000_AddGl2MessageIdFieldAliasForEventsES7(elasticsearch.restHighLevelClient());
    }

    @Test
    public void addsFieldAliasForMatchingIndices() {
        createIndicesWithIdMapping("aaa_0", "aaa_1", "bbb_0");

        sut.addGl2MessageIdFieldAlias(ImmutableSet.of("aaa", "bbb"));

        assertFieldAliasIsPresentForIndices("aaa_0", "aaa_1", "bbb_0");
    }

    @Test
    public void doesNotAddFieldAliasForNonMatchingIndices() {
        createIndicesWithIdMapping("aaa_0", "aaa_1", "bbb_0");

        sut.addGl2MessageIdFieldAlias(ImmutableSet.of("bbb"));

        assertFieldAliasIsNotPresentForIndices("aaa_0", "aaa_1");
    }

    private void assertFieldAliasIsPresentForIndices(String... indices) {
        final GetIndexResponse response = getIndices(indices);

        for (String index : indices) {
            assertMappingsContainFieldAliasForIndex(response.getMappings(), index);
        }
    }

    private GetIndexResponse getIndices(String[] indices) {
        return elasticsearch.elasticsearchClient()
                .execute((c, opt) -> c.indices().get(new GetIndexRequest(indices), opt));
    }

    private void assertMappingsContainFieldAliasForIndex(Map<String, MappingMetadata> mappings, String index) {
        final Map<String, Object> properties = propertiesFrom(mappings, index);

        assertThat(properties).as(FIELD_GL2_MESSAGE_ID + " should be present in mapping for " + index)
                .containsKey(FIELD_GL2_MESSAGE_ID);
        assertThat(properties.get(FIELD_GL2_MESSAGE_ID)).as("field alias definition for " + index + " should be valid")
                .isEqualTo(V20200730000000_AddGl2MessageIdFieldAliasForEventsES7.aliasMapping());
    }

    private Map<String, Object> propertiesFrom(Map<String, MappingMetadata> mappings, String index) {
        //noinspection unchecked
        return (Map<String, Object>) mappings.get(index).getSourceAsMap().get("properties");
    }

    private void assertFieldAliasIsNotPresentForIndices(String... indices) {
        final GetIndexResponse response = getIndices(indices);

        for (String index : indices) {
            assertMappingsDontContainFieldAliasForIndex(response.getMappings(), index);
        }
    }

    private void assertMappingsDontContainFieldAliasForIndex(Map<String, MappingMetadata> mappings, String index) {
        final Map<String, Object> properties = propertiesFrom(mappings, index);

        assertThat(properties).as(FIELD_GL2_MESSAGE_ID + " should NOT be present in mapping for " + index)
                .doesNotContainKey(FIELD_GL2_MESSAGE_ID);
    }

    private void createIndicesWithIdMapping(String... indices) {
        for (String index : indices) {
            indicesAdapter.create(index, IndexSettings.create(1, 0), null, null);
        }

        final PutMappingRequest putMappingRequest = new PutMappingRequest(indices).source(idMapping());
        final AcknowledgedResponse acknowledgedResponse = elasticsearch.elasticsearchClient()
                .execute((c, opt) -> c.indices().putMapping(putMappingRequest, opt));

        if (!acknowledgedResponse.isAcknowledged()) {
            throw new RuntimeException("Failed to add 'id' mapping for indices " + Arrays.toString(indices));
        }
    }

    private Map<String, Object> idMapping() {
        return map("properties", map("id", map("type", "keyword")));
    }

    private ImmutableMap<String, Object> map(String key, Object value) {
        return ImmutableMap.of(key, value);
    }

    private IndicesAdapterES7 createIndicesAdapter() {
        final ElasticsearchClient client = elasticsearch.elasticsearchClient();
        return new IndicesAdapterES7(
                client,
                new StatsApi(objectMapper, client),
                new CatApi(objectMapper, client),
                new ClusterStateApi(objectMapper, client)
        );
    }
}
