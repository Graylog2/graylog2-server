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
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.State;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

public class IndicesIT extends ElasticsearchBaseTest {
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

    @SuppressWarnings("UnstableApiUsage")
    private EventBus eventBus;
    private Indices indices;
    private IndexMappingFactory indexMappingFactory;

    @Before
    public void setUp() {
        //noinspection UnstableApiUsage
        eventBus = new EventBus("indices-test");
        final Node node = new Node(jestClient());
        indexMappingFactory = new IndexMappingFactory(node);
        indices = new Indices(jestClient(),
                new ObjectMapperProvider().get(),
                indexMappingFactory,
                new Messages(new MetricRegistry(), jestClient(), new InMemoryProcessingStatusRecorder()),
                mock(NodeId.class),
                new NullAuditEventSender(),
                eventBus);
    }

    @Test
    public void testDelete() {
        final String index = client().createRandomIndex("indices_it_");
        indices.delete(index);

        assertThat(client().indicesExists(index)).isFalse();
    }

    @Test
    public void testClose() {
        final String index = client().createRandomIndex("indices_it_");

        assertThat(getIndexState(index)).isEqualTo("open");

        indices.close(index);

        assertThat(getIndexState(index)).isEqualTo("close");
    }

    private String getIndexState(String index) {
        final State beforeRequest = new State.Builder().indices(index).withMetadata().build();

        final JestResult beforeResponse = client().executeWithExpectedSuccess(beforeRequest, "failed to get index metadata");

        return beforeResponse.getJsonObject().path("metadata").path("indices").path(index).path("state").asText();
    }

    @Test
    public void testAliasExists() {
        final String index = client().createRandomIndex("indices_it_");
        final String alias = "graylog_alias_exists";
        assertThat(indices.aliasExists(alias)).isFalse();

        client().addAliasMapping(index, alias);

        assertThat(indices.aliasExists(alias)).isTrue();
        assertThat(indices.exists(alias)).isFalse();
    }

    @Test
    public void testAliasExistsForIndex() {
        final String indexName = client().createRandomIndex("indices_it_");

        assertThat(indices.aliasExists(indexName)).isFalse();
    }

    @Test
    public void testIndexIfIndexExists() {
        final String indexName = client().createRandomIndex("indices_it_");

        assertThat(indices.exists(indexName)).isTrue();
    }

    @Test
    public void testExistsIfIndexDoesNotExist() {
        final String indexNotAlias = "graylog_index_does_not_exist";
        assertThat(indices.exists(indexNotAlias)).isFalse();
    }

    @Test
    public void testAliasTarget() {
        final String index = client().createRandomIndex("indices_it_");
        final String alias = "graylog_alias_target";
        assertThat(indices.aliasTarget(alias)).isEmpty();

        client().addAliasMapping(index, alias);

        assertThat(indices.aliasTarget(alias)).contains(index);
    }

    @Test
    public void testTimestampStatsOfIndex() {
        importFixture("IndicesIT.json");

        IndexRangeStats stats = indices.indexRangeStatsOfIndex(INDEX_NAME);

        assertThat(stats.min()).isEqualTo(new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC));
    }

    @Test
    public void testTimestampStatsOfIndexWithEmptyIndex() {
        final String indexName = client().createRandomIndex("indices_it_");

        IndexRangeStats stats = indices.indexRangeStatsOfIndex(indexName);

        assertThat(stats.min()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
    }

    @Test(expected = IndexNotFoundException.class)
    public void testTimestampStatsOfIndexWithClosedIndex() {
        final String index = client().createRandomIndex("indices_it_");

        client().closeIndex(index);

        indices.indexRangeStatsOfIndex(index);
    }

    @Test(expected = IndexNotFoundException.class)
    public void testTimestampStatsOfIndexWithNonExistingIndex() {
        indices.indexRangeStatsOfIndex("does-not-exist");
    }

    @Test
    public void testCreateEnsuresIndexTemplateExists() {
        final String indexName = "index_template_test";
        final String templateName = indexSetConfig.indexTemplateName();

        final JsonNode beforeTemplates = client().getTemplates();
        assertThat(beforeTemplates.path(templateName).isMissingNode()).isTrue();

        indices.create(indexName, indexSet);

        final JsonNode afterTemplates = client().getTemplates();
        assertThat(afterTemplates.path(templateName).isObject()).isTrue();

        final JsonNode templateMetaData = afterTemplates.path(templateName);
        assertThat(templateMetaData.path("mappings")).hasSize(1);
        assertThat(templateMetaData.path("mappings").path(IndexMapping.TYPE_MESSAGE).isObject()).isTrue();
    }

    @Test
    public void testCreateOverwritesIndexTemplate() {
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

        client().putTemplate(templateName, templateSource);

        final JsonNode beforeTemplate = client().getTemplate(templateName);
        final JsonNode actualBeforeMapping = beforeTemplate.path(templateName).path("mappings").path(IndexMapping.TYPE_MESSAGE);
        final Map<String, Object> actualMapping = mapper.convertValue(actualBeforeMapping, TypeReferences.MAP_STRING_OBJECT);
        assertThat(actualMapping).isEqualTo(beforeMapping);

        indices.create("index_template_test", indexSet);

        final JsonNode afterTemplate = client().getTemplate(templateName);
        assertThat(afterTemplate.path(templateName).isObject()).isTrue();

        final JsonNode actualAfterMapping = afterTemplate.path(templateName).path("mappings");
        assertThat(actualAfterMapping).hasSize(1);
        assertThat(actualAfterMapping.path(IndexMapping.TYPE_MESSAGE).isObject()).isTrue();

        final Map<String, Object> mapping = mapper.convertValue(actualAfterMapping, TypeReferences.MAP_STRING_OBJECT);
        final Map<String, Object> expectedTemplate = indexMappingFactory.createIndexMapping(IndexSetConfig.TemplateType.MESSAGES).toTemplate(indexSetConfig, indexSet.getIndexWildcard());
        assertThat(mapping).isEqualTo(expectedTemplate.get("mappings"));
    }

    @Test
    public void indexCreationDateReturnsIndexCreationDateOfExistingIndexAsDateTime() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final String indexName = client().createRandomIndex("indices_it_");

        indices.indexCreationDate(indexName).ifPresent(
                indexCreationDate -> org.assertj.jodatime.api.Assertions.assertThat(indexCreationDate).isAfterOrEqualTo(now)
        );
    }

    @Test
    public void indexCreationDateReturnsEmptyOptionalForNonExistingIndex() {
        assertThat(indices.indexCreationDate("index_missing")).isEmpty();
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
        final JsonNode existingTemplate = client().getTemplate(customTemplateName);
        assertThat(existingTemplate.path(customTemplateName).isObject()).isTrue();

        // Create index with custom template
        indices.create(testIndexName, indexSet);
        client().waitForGreenStatus(testIndexName);

        // Check index mapping
        final JsonNode indexMappings = client().getMapping(testIndexName);
        final JsonNode mapping = indexMappings.path(testIndexName).path("mappings").path(IndexMapping.TYPE_MESSAGE);

        assertThat(mapping.path("_source").path("enabled")).isEqualTo(BooleanNode.getFalse());
        assertThat(mapping.path("properties").path("source").path("type")).isEqualTo(new TextNode("text"));
    }

    @Test
    public void closePostsIndicesClosedEvent() {
        final IndicesEventListener listener = new IndicesEventListener();
        eventBus.register(listener);

        final String index = client().createRandomIndex("indices_it_");

        indices.close(index);


        assertThat(listener.indicesClosedEvents).containsOnly(IndicesClosedEvent.create(index));
        assertThat(listener.indicesDeletedEvents).isEmpty();
        assertThat(listener.indicesReopenedEvents).isEmpty();
    }

    @Test
    public void deletePostsIndicesDeletedEvent() {
        final IndicesEventListener listener = new IndicesEventListener();
        eventBus.register(listener);

        final String index = client().createRandomIndex("indices_it_");

        indices.delete(index);

        assertThat(listener.indicesDeletedEvents).containsOnly(IndicesDeletedEvent.create(index));
        assertThat(listener.indicesClosedEvents).isEmpty();
        assertThat(listener.indicesReopenedEvents).isEmpty();
    }

    @Test
    public void reopenIndexPostsIndicesReopenedEvent() {
        final IndicesEventListener listener = new IndicesEventListener();
        eventBus.register(listener);

        final String index = client().createRandomIndex("indices_it_");

        client().closeIndex(index);

        indices.reopenIndex(index);

        assertThat(listener.indicesReopenedEvents).containsOnly(IndicesReopenedEvent.create(index));
        assertThat(listener.indicesClosedEvents).isEmpty();
        assertThat(listener.indicesDeletedEvents).isEmpty();
    }

    @SuppressWarnings("UnstableApiUsage")
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
    public void getIndices() {
        final IndexSet indexSet = new TestIndexSet(indexSetConfig.toBuilder().indexPrefix("indices_it").build());
        final String index1 = client().createRandomIndex("indices_it_");
        final String index2 = client().createRandomIndex("indices_it_");

        client().closeIndex(index2);

        assertThat(indices.getIndices(indexSet))
                .containsOnly(index1, index2);
        assertThat(indices.getIndices(indexSet, "open", "close"))
                .containsOnly(index1, index2);
        assertThat(indices.getIndices(indexSet, "open"))
                .containsOnly(index1);
        assertThat(indices.getIndices(indexSet, "close"))
                .containsOnly(index2);
    }
}
