package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.stats.StatsApi;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.IndicesIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;

import java.util.Map;

public class IndicesES7IT extends IndicesIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    @Before
    public void setUp() {
        super.setUp();
    }

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return elasticsearch;
    }

    @Override
    protected IndicesAdapter indicesAdapter() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new IndicesAdapterES7(
                elasticsearch.elasticsearchClient(),
                new StatsApi(objectMapper),
                new CatApi(objectMapper, elasticsearch.elasticsearchClient())
        );
    }

    @Override
    protected Map<String, Object> createTemplateFor(String indexWildcard) {
        final Map<String, Object> beforeMapping = ImmutableMap.of(
                "_source", ImmutableMap.of("enabled", false),
                "properties", ImmutableMap.of("message",
                        ImmutableMap.of("type", "text")));


        return ImmutableMap.of(
                "template", indexWildcard,
                "mappings", beforeMapping
        );
    }
}
