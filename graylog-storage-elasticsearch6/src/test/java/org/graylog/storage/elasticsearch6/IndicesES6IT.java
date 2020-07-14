package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.graylog.storage.elasticsearch6.testing.ClientES6;
import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.IndicesIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.storage.elasticsearch6.testing.TestUtils.jestClient;
import static org.junit.Assume.assumeTrue;

public class IndicesES6IT extends IndicesIT {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected IndicesAdapter indicesAdapter() {
        return new IndicesAdapterES6(jestClient(elasticsearch),
                new ObjectMapperProvider().get(),
                new IndexingHelper());
    }

    @Override
    protected NodeAdapter createNodeAdapter() {
        return new NodeAdapterES6(jestClient(elasticsearch));
    }

    @Override
    protected Map<String, Object> createTemplateFor(String indexWildcard, Map<String, Object> mapping) {
        return ImmutableMap.of(
                "template", indexWildcard,
                "mappings", ImmutableMap.of(IndexMapping.TYPE_MESSAGE, mapping)
        );
    }

    @Test
    public void testIndexTemplateCanBeOverridden_Elasticsearch5() {
        assumeTrue(elasticsearchVersion().getMajorVersion() == 5);

        final String testIndexName = "graylog_override_template";
        final String customTemplateName = "custom-template";

        // Create custom index template
        final Map<String, Object> customMapping = ImmutableMap.of(
                "_source", ImmutableMap.of("enabled", false),
                "properties", ImmutableMap.of("source", ImmutableMap.of("type", "text")));
        final Map<String, Object> templateSource = ImmutableMap.of(
                "template", indexSet.getIndexWildcard(),
                "order", 1,
                "mappings", ImmutableMap.of(IndexMapping.TYPE_MESSAGE, customMapping)
        );

        client().putTemplate(customTemplateName, templateSource);

        // Validate existing index templates
        final JsonNode existingTemplate = ((ClientES6)client()).getTemplate(customTemplateName);
        assertThat(existingTemplate.path(customTemplateName).isObject()).isTrue();

        // Create index with custom template
        indices.create(testIndexName, indexSet);
        client().waitForGreenStatus(testIndexName);

        assertThat(client().isSourceEnabled(testIndexName)).isFalse();
        assertThat(client().fieldType(testIndexName, "source")).isEqualTo("text");
    }
}
