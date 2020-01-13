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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.joschi.nosqlunit.elasticsearch.http.ElasticsearchConfiguration;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.State;
import org.graylog2.ElasticsearchBase;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.events.IndicesClosedEvent;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.indices.events.IndicesReopenedEvent;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.system.processing.InMemoryProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

public class IndicesIT extends ElasticsearchBase {
    private static final String INDEX_NAME = "graylog_0";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final IndexSetConfig indexSetConfig = IndexSetConfig.builder()
            .id("index-set-1")
            .title("Index set 1")
            .description("For testing")
            .indexPrefix("graylog")
            .creationDate(ZonedDateTime.now())
            .shards(1)
            .replicas(0)
            .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
            .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
            .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
            .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
            .indexAnalyzer("standard")
            .indexTemplateName("template-1")
            .indexOptimizationMaxNumSegments(1)
            .indexOptimizationDisabled(false)
            .build();
    private static final IndexSet indexSet = new TestIndexSet(indexSetConfig);

    private EventBus eventBus;
    private Indices indices;
    private IndexMappingFactory indexMappingFactory;

    @Override
    protected ElasticsearchConfiguration.Builder elasticsearchConfiguration() {
        final Map<String, Map<String, Object>> messageTemplates = Collections.singletonMap("graylog-test-internal", indexMapping().messageTemplate("*", "standard"));
        return super.elasticsearchConfiguration()
                .indexTemplates(messageTemplates)
                .createIndices(false)
                .deleteAllIndices(true);
    }

    @Before
    public void setUp() throws Exception {
        eventBus = new EventBus("indices-test");
        final Node node = new Node(client());
        indexMappingFactory = new IndexMappingFactory(node);
        indices = new Indices(client(),
                new ObjectMapperProvider().get(),
                indexMappingFactory,
                new Messages(new MetricRegistry(), client(), new InMemoryProcessingStatusRecorder(), true),
                mock(NodeId.class),
                new NullAuditEventSender(),
                eventBus);
    }

    @Test
    public void testDelete() throws Exception {
        final String index = createRandomIndex("indices_it_");
        indices.delete(index);

        assertThat(indicesExists(index)).isFalse();
    }

    @Test
    public void testClose() throws Exception {
        final String index = createRandomIndex("indices_it_");
        try {
            final State beforeRequest = new State.Builder().indices(index).withMetadata().build();
            final JestResult beforeResponse = client().execute(beforeRequest);
            assertSucceeded(beforeResponse);
            final JsonNode beforeOpened = beforeResponse.getJsonObject().path("metadata").path("indices").path(index);
            assertThat(beforeOpened.path("state").asText()).isEqualTo("open");

            indices.close(index);

            final State afterRequest = new State.Builder().indices(index).withMetadata().build();
            final JestResult afterResponse = client().execute(afterRequest);
            assertSucceeded(afterResponse);
            final JsonNode afterOpened = afterResponse.getJsonObject().path("metadata").path("indices").path(index);
            assertThat(afterOpened.path("state").asText()).isEqualTo("close");
        } finally {
            deleteIndex(index);
        }
    }

    @Test
    public void testAliasExists() throws Exception {
        final String index = createRandomIndex("indices_it_");
        final String alias = "graylog_alias_exists";
        assertThat(indices.aliasExists(alias)).isFalse();

        try {
            addAliasMapping(index, alias);

            assertThat(indices.aliasExists(alias)).isTrue();
            assertThat(indices.exists(alias)).isFalse();
        } finally {
            removeAliasMapping(index, alias);
            deleteIndex(index);
        }
    }

    @Test
    public void testAliasExistsForIndex() throws Exception {
        final String indexName = createRandomIndex("indices_it_");
        try {
            assertThat(indices.aliasExists(indexName)).isFalse();
        } finally {
            deleteIndex(indexName);
        }
    }

    @Test
    public void testIndexIfIndexExists() throws Exception {
        final String indexName = createRandomIndex("indices_it_");

        try {
            assertThat(indices.exists(indexName)).isTrue();
        } finally {
            deleteIndex(indexName);
        }
    }

    @Test
    public void testExistsIfIndexDoesNotExist() throws Exception {
        final String indexNotAlias = "graylog_index_does_not_exist";
        assertThat(indices.exists(indexNotAlias)).isFalse();
    }

    @Test
    public void testAliasTarget() throws Exception {
        final String index = createRandomIndex("indices_it_");
        final String alias = "graylog_alias_target";
        assertThat(indices.aliasTarget(alias)).isEmpty();

        try {
            addAliasMapping(index, alias);

            assertThat(indices.aliasTarget(alias)).contains(index);
        } finally {
            removeAliasMapping(index, alias);
            deleteIndex(index);
        }
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testTimestampStatsOfIndex() throws Exception {
        IndexRangeStats stats = indices.indexRangeStatsOfIndex(INDEX_NAME);

        assertThat(stats.min()).isEqualTo(new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC));
    }

    @Test
    public void testTimestampStatsOfIndexWithEmptyIndex() throws Exception {
        final String indexName = createRandomIndex("indices_it_");

        IndexRangeStats stats = indices.indexRangeStatsOfIndex(indexName);

        assertThat(stats.min()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
    }

    @Test(expected = IndexNotFoundException.class)
    public void testTimestampStatsOfIndexWithClosedIndex() throws Exception {
        final String index = createRandomIndex("indices_it_");
        try {
            closeIndex(index);

            indices.indexRangeStatsOfIndex(index);
        } finally {
            deleteIndex(index);
        }
    }

    @Test(expected = IndexNotFoundException.class)
    public void testTimestampStatsOfIndexWithNonExistingIndex() throws Exception {
        indices.indexRangeStatsOfIndex("does-not-exist");
    }

    @Test
    public void testCreateEnsuresIndexTemplateExists() throws Exception {
        final String indexName = "index_template_test";
        final String templateName = indexSetConfig.indexTemplateName();

        try {
            final JsonNode beforeTemplates = getTemplates();
            assertThat(beforeTemplates.path(templateName).isMissingNode()).isTrue();

            indices.create(indexName, indexSet);

            final JsonNode afterTemplates = getTemplates();
            assertThat(afterTemplates.path(templateName).isObject()).isTrue();

            final JsonNode templateMetaData = afterTemplates.path(templateName);
            assertThat(templateMetaData.path("mappings")).hasSize(1);
            assertThat(templateMetaData.path("mappings").path(IndexMapping.TYPE_MESSAGE).isObject()).isTrue();
        } finally {
            deleteTemplate(templateName);
            deleteIndex(indexName);
        }
    }

    @Test
    public void testCreateOverwritesIndexTemplate() throws Exception {
        final ObjectMapper mapper = new ObjectMapperProvider().get();
        final String templateName = indexSetConfig.indexTemplateName();

        final Map<String, Object> beforeMapping = ImmutableMap.of(
                "_source", ImmutableMap.of("enabled", false),
                "properties", ImmutableMap.of("message",
                        ImmutableMap.of(
                                "type", "string",
                                "index", "not_analyzed")));

        final Map<String, Object> templateSource = ImmutableMap.of(
                "template", indexSet.getIndexWildcard(),
                "mappings", ImmutableMap.of(IndexMapping.TYPE_MESSAGE, beforeMapping)
        );

        try {
            putTemplate(templateName, templateSource);

            final JsonNode beforeTemplate = getTemplate(templateName);
            final JsonNode actualBeforeMapping = beforeTemplate.path(templateName).path("mappings").path(IndexMapping.TYPE_MESSAGE);
            final Map<String, Object> actualMapping = mapper.convertValue(actualBeforeMapping, TypeReferences.MAP_STRING_OBJECT);
            assertThat(actualMapping).isEqualTo(beforeMapping);

            indices.create("index_template_test", indexSet);

            final JsonNode afterTemplate = getTemplate(templateName);
            assertThat(afterTemplate.path(templateName).isObject()).isTrue();

            final JsonNode actualAfterMapping = afterTemplate.path(templateName).path("mappings");
            assertThat(actualAfterMapping).hasSize(1);
            assertThat(actualAfterMapping.path(IndexMapping.TYPE_MESSAGE).isObject()).isTrue();

            final Map<String, Object> mapping = mapper.convertValue(actualAfterMapping, TypeReferences.MAP_STRING_OBJECT);
            final Map<String, Object> expectedTemplate = indexMappingFactory.createIndexMapping(IndexSetConfig.TemplateType.MESSAGES).toTemplate(indexSetConfig, indexSet.getIndexWildcard());
            assertThat(mapping).isEqualTo(expectedTemplate.get("mappings"));
        } finally {
            deleteTemplate(templateName);
            deleteIndex("index_template_test");
        }
    }

    @Test
    public void indexCreationDateReturnsIndexCreationDateOfExistingIndexAsDateTime() throws IOException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final String indexName = createRandomIndex("indices_it_");
        try {
            indices.indexCreationDate(indexName).ifPresent(
                    indexCreationDate -> org.assertj.jodatime.api.Assertions.assertThat(indexCreationDate).isAfterOrEqualTo(now)
            );
        } finally {
            deleteIndex(indexName);
        }
    }

    @Test
    public void indexCreationDateReturnsEmptyOptionalForNonExistingIndex() {
        assertThat(indices.indexCreationDate("index_missing")).isEmpty();
    }

    @Test
    public void testIndexTemplateCanBeOverridden_Elasticsearch5() throws Exception {
        assumeTrue(getElasticsearchVersion().getMajorVersion() == 5);

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

        putTemplate(customTemplateName, templateSource);

        try {
            // Validate existing index templates
            final JsonNode existingTemplate = getTemplate(customTemplateName);
            assertThat(existingTemplate.path(customTemplateName).isObject()).isTrue();

            // Create index with custom template
            indices.create(testIndexName, indexSet);
            waitForGreenStatus(testIndexName);

            // Check index mapping
            final JsonNode indexMappings = getMapping(testIndexName);
            final JsonNode mapping = indexMappings.path(testIndexName).path("mappings").path(IndexMapping.TYPE_MESSAGE);

            assertThat(mapping.path("_source").path("enabled")).isEqualTo(BooleanNode.getFalse());
            assertThat(mapping.path("properties").path("source").path("type")).isEqualTo(new TextNode("text"));
        } finally {
            // Clean up
            deleteTemplate(customTemplateName);
            deleteIndex(testIndexName);
        }
    }

    @Test
    public void closePostsIndicesClosedEvent() throws IOException {
        final IndicesEventListener listener = new IndicesEventListener();
        eventBus.register(listener);

        final String index = createRandomIndex("indices_it_");
        try {
            indices.close(index);
        } finally {
            deleteIndex(index);
        }

        assertThat(listener.indicesClosedEvents).containsOnly(IndicesClosedEvent.create(index));
        assertThat(listener.indicesDeletedEvents).isEmpty();
        assertThat(listener.indicesReopenedEvents).isEmpty();
    }

    @Test
    public void deletePostsIndicesDeletedEvent() throws IOException {
        final IndicesEventListener listener = new IndicesEventListener();
        eventBus.register(listener);

        final String index = createRandomIndex("indices_it_");
        try {
            indices.delete(index);
        } finally {
            deleteIndex(index);
        }

        assertThat(listener.indicesDeletedEvents).containsOnly(IndicesDeletedEvent.create(index));
        assertThat(listener.indicesClosedEvents).isEmpty();
        assertThat(listener.indicesReopenedEvents).isEmpty();
    }

    @Test
    public void reopenIndexPostsIndicesReopenedEvent() throws IOException {
        final IndicesEventListener listener = new IndicesEventListener();
        eventBus.register(listener);

        final String index = createRandomIndex("indices_it_");
        try {
            closeIndex(index);

            indices.reopenIndex(index);
        } finally {
            deleteIndex(index);
        }

        assertThat(listener.indicesReopenedEvents).containsOnly(IndicesReopenedEvent.create(index));
        assertThat(listener.indicesClosedEvents).isEmpty();
        assertThat(listener.indicesDeletedEvents).isEmpty();
    }

    public static final class IndicesEventListener {
        final List<IndicesClosedEvent> indicesClosedEvents = Collections.synchronizedList(new ArrayList<>());
        final List<IndicesDeletedEvent> indicesDeletedEvents = Collections.synchronizedList(new ArrayList<>());
        final List<IndicesReopenedEvent> indicesReopenedEvents = Collections.synchronizedList(new ArrayList<>());

        @Subscribe
        @SuppressWarnings("unused")
        public void handleIndicesClosedEvent(IndicesClosedEvent event) {
            indicesClosedEvents.add(event);
        }

        @Subscribe
        @SuppressWarnings("unused")
        public void handleIndicesDeletedEvent(IndicesDeletedEvent event) {
            indicesDeletedEvents.add(event);
        }

        @Subscribe
        @SuppressWarnings("unused")
        public void handleIndicesReopenedEvent(IndicesReopenedEvent event) {
            indicesReopenedEvents.add(event);
        }
    }

    @Test
    public void getIndices() throws Exception {
        final IndexSet indexSet = new TestIndexSet(indexSetConfig.toBuilder().indexPrefix("indices_it").build());
        final String index1 = createRandomIndex("indices_it_");
        final String index2 = createRandomIndex("indices_it_");

        try {
            closeIndex(index2);

            assertThat(indices.getIndices(indexSet))
                    .containsOnly(index1, index2);
            assertThat(indices.getIndices(indexSet, "open", "close"))
                    .containsOnly(index1, index2);
            assertThat(indices.getIndices(indexSet, "open"))
                    .containsOnly(index1);
            assertThat(indices.getIndices(indexSet, "close"))
                    .containsOnly(index2);
        } finally {
            deleteIndex(index1);
            deleteIndex(index2);
        }

        assertThat(indices.getIndices(indexSet)).isEmpty();
    }
}
