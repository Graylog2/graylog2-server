/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.indexer.indices;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.assertj.jodatime.api.Assertions;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetStatsCreator;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.events.IndicesClosedEvent;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.indices.events.IndicesReopenedEvent;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class IndicesIT extends ElasticsearchBaseTest {
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
    protected static final IndexSet indexSet = new TestIndexSet(indexSetConfig);

    @SuppressWarnings("UnstableApiUsage")
    private EventBus eventBus;
    protected Indices indices;

    protected abstract IndicesAdapter indicesAdapter();

    @Before
    public void setUp() {
        //noinspection UnstableApiUsage
        eventBus = new EventBus("indices-test");
        final Node node = new Node(createNodeAdapter());
        final IndexMappingFactory indexMappingFactory = new IndexMappingFactory(node);
        indices = new Indices(
                indexMappingFactory,
                mock(NodeId.class),
                new NullAuditEventSender(),
                eventBus,
                indicesAdapter()
        );
    }

    protected abstract NodeAdapter createNodeAdapter();

    @Test
    public void testDelete() {
        final String index = client().createRandomIndex("indices_it_");
        indices.delete(index);

        assertThat(client().indicesExists(index)).isFalse();
    }

    @Test
    public void testClose() {
        final String index = client().createRandomIndex("indices_it_");

        assertThat(indices.isOpen(index)).isTrue();

        indices.close(index);

        assertThat(indices.isClosed(index)).isTrue();
    }

    @Test
    public void findClosedIndices() {
        final String index1 = client().createRandomIndex("indices_it_");
        client().closeIndex(index1);
        final String index2 = client().createRandomIndex("otherindices_it_");
        client().closeIndex(index2);
        client().createRandomIndex("evenmoreindices_it_");

        final Set<String> closedIndices = indices.getClosedIndices(Collections.singleton("*"));

        assertThat(closedIndices).containsExactlyInAnyOrder(index1, index2);
    }

    @Test
    public void aliasExistsReturnsIfGivenIndexNameIsIndexOrAlias() {
        final String index = client().createRandomIndex("indices_it_");
        final String alias = "graylog_alias_exists";
        assertThat(indices.aliasExists(alias)).isFalse();

        client().addAliasMapping(index, alias);

        assertThat(indices.aliasExists(alias)).isTrue();
        assertThat(indices.exists(alias)).isFalse();
    }

    @Test
    public void aliasExistsReturnsIfGivenIndexHasAlias() {
        final String indexName = client().createRandomIndex("indices_it_");

        assertThat(indices.aliasExists(indexName)).isFalse();
    }

    @Test
    public void existsIndicatesPresenceOfGivenIndex() {
        final String indexName = client().createRandomIndex("indices_it_");

        assertThat(indices.exists(indexName)).isTrue();
    }

    @Test
    public void existsReturnsFalseIfGivenIndexDoesNotExists() {
        final String indexNotAlias = "graylog_index_does_not_exist";
        assertThat(indices.exists(indexNotAlias)).isFalse();
    }

    @Test
    public void aliasTargetReturnsListOfTargetsGivenAliasIsPointingTo() {
        final String index = client().createRandomIndex("indices_it_");
        final String alias = "graylog_alias_target";
        assertThat(indices.aliasTarget(alias)).isEmpty();

        client().addAliasMapping(index, alias);

        assertThat(indices.aliasTarget(alias)).contains(index);
    }

    @Test
    public void indexRangeStatsOfIndexReturnsMinMaxTimestampsForGivenIndex() {
        importFixture("org/graylog2/indexer/indices/IndicesIT.json");

        IndexRangeStats stats = indices.indexRangeStatsOfIndex(INDEX_NAME);

        assertThat(stats.min()).isEqualTo(new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC));
    }

    @Test
    public void indexRangeStatsWorksForEmptyIndex() {
        final String indexName = client().createRandomIndex("indices_it_");

        IndexRangeStats stats = indices.indexRangeStatsOfIndex(indexName);

        assertThat(stats.min()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
    }

    @Test(expected = IndexNotFoundException.class)
    public void indexRangeStatsThrowsExceptionIfIndexIsClosed() {
        final String index = client().createRandomIndex("indices_it_");

        client().closeIndex(index);

        indices.indexRangeStatsOfIndex(index);
    }

    @Test(expected = IndexNotFoundException.class)
    public void indexRangeStatsThrowsExceptionIfIndexDoesNotExists() {
        indices.indexRangeStatsOfIndex("does-not-exist");
    }

    @Test
    public void createEnsuresIndexTemplateExists() {
        final String indexName = "index_template_test";
        final String templateName = indexSetConfig.indexTemplateName();

        assertThat(client().templateExists(templateName)).isFalse();

        indices.create(indexName, indexSet);

        assertThat(client().templateExists(templateName)).isTrue();
        assertThat(client().fieldType("index_template_test", "message")).isEqualTo("text");
    }

    protected abstract Map<String, Object> createTemplateFor(String indexWildcard, Map<String, Object> beforeMapping);

    @Test
    public void createOverwritesIndexTemplate() {
        final String templateName = indexSetConfig.indexTemplateName();

        final Map<String, Object> beforeMapping = ImmutableMap.of(
                "_source", ImmutableMap.of("enabled", false),
                "properties", ImmutableMap.of("message",
                        ImmutableMap.of("type", "text")));

        final Map<String, Object> templateSource = createTemplateFor(indexSet.getIndexWildcard(), beforeMapping);

        client().putTemplate(templateName, templateSource);

        indices.create("index_template_test", indexSet);

        assertThat(client().fieldType("index_template_test", "message")).isEqualTo("text");
    }

    @Test
    public void indexCreationDateReturnsIndexCreationDateOfExistingIndexAsDateTime() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final String indexName = client().createRandomIndex("indices_it_");

        final Optional<DateTime> indexCreationDate = indices.indexCreationDate(indexName);
        assertThat(indexCreationDate).isNotEmpty()
                .hasValueSatisfying(date -> Assertions.assertThat(date).isEqualToIgnoringMillis(now));
    }

    @Test
    public void indexCreationDateReturnsEmptyOptionalForNonExistingIndex() {
        assertThat(indices.indexCreationDate("index_missing")).isEmpty();
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

    @Test
    public void storeSizeInBytesReturnsValue() {
        final String index = client().createRandomIndex("foo");

        final Optional<Long> storeSizeInBytes = indices.getStoreSizeInBytes(index);

        assertThat(storeSizeInBytes).isNotEmpty();
    }

    @Test
    public void retrievesCreationTimeOfIndexInUTC() {
        final String index = client().createRandomIndex("foo");

        final Optional<DateTime> creationDate = indices.indexCreationDate(index);

        assertThat(creationDate).hasValueSatisfying(dt ->
                assertThat(dt.getZone()).isEqualTo(DateTimeZone.UTC));
    }

    @Test
    public void retrievesAllAliasesForIndex() {
        final String index1 = client().createRandomIndex("foo-");
        final String index2 = client().createRandomIndex("foo-");

        client().addAliasMapping(index1, "alias1");
        client().addAliasMapping(index2, "alias2");
        client().addAliasMapping(index2, "alias3");

        final Map<String, Set<String>> indexNamesAndAliases = indices.getIndexNamesAndAliases("foo-*");

        assertThat(indexNamesAndAliases)
                .containsAllEntriesOf(
                        ImmutableMap.of(
                                index1, Collections.singleton("alias1"),
                                index2, ImmutableSet.of("alias2", "alias3")
                        )
                );
    }

    @Test
    public void retrieveIndexStatisticsForIndices() {
        final String index = client().createRandomIndex("indices_it_");

        final Set<IndexStatistics> indicesStats = indices.getIndicesStats(Collections.singleton(index));

        assertThat(indicesStats).isNotEmpty();
    }

    @Test
    public void cyclingDeflectorMovesAliasFromOldToNewTarget() {
        final String deflector = "indices_it_deflector";

        final String index1 = client().createRandomIndex("indices_it_");
        final String index2 = client().createRandomIndex("indices_it_");

        client().addAliasMapping(index1, deflector);

        assertThat(indices.aliasTarget(deflector)).hasValue(index1);

        indices.cycleAlias(deflector, index2, index1);

        assertThat(indices.aliasTarget(deflector)).hasValue(index2);
    }

    @Test
    public void retrievingIndexStatsForWildcard() {
        final IndexSetStatsCreator indexSetStatsCreator = new IndexSetStatsCreator(indices);
        final String indexPrefix = "indices_wildcard_";
        final String wildcard = indexPrefix + "*";
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getIndexWildcard()).thenReturn(wildcard);

        client().createRandomIndex(indexPrefix);
        client().createRandomIndex(indexPrefix);

        final IndexSetStats indexSetStats = indexSetStatsCreator.getForIndexSet(indexSet);

        assertThat(indexSetStats.indices()).isEqualTo(2L);
        assertThat(indexSetStats.size()).isNotZero();
    }
}
