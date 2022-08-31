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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.codec.binary.Base64;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.IndicesAdapterES7;
import org.graylog.storage.elasticsearch7.NodeAdapterES7;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.cluster.ClusterStateApi;
import org.graylog.storage.elasticsearch7.stats.StatsApi;
import org.graylog.testing.ContainerMatrixElasticsearchITBaseTest;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.indexer.IgnoreIndexTemplate;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetStatsCreator;
import org.graylog2.indexer.IndexTemplateNotFoundException;
import org.graylog2.indexer.MessageIndexTemplateProvider;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
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
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// these tests only test the SearchServer, so there is only one MongoDB-version necessary (needed, to launch the tests)
@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, mongoVersions = MongodbServer.MONGO4)
public class IndicesIT extends ContainerMatrixElasticsearchITBaseTest {
    private static final String INDEX_NAME = "graylog_0";
    private final Set<String> indicesToCleanUp = new HashSet<>();

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

    public IndicesIT(SearchServerInstance elasticsearch) {
        super(elasticsearch);
    }

    protected IndicesAdapter indicesAdapter() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final ElasticsearchClient client = elasticsearchClient();
        return new IndicesAdapterES7(
                client,
                new StatsApi(objectMapper, client),
                new CatApi(objectMapper, client),
                new ClusterStateApi(objectMapper, client)
        );
    }

    protected NodeAdapter createNodeAdapter() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new NodeAdapterES7(elasticsearchClient(), objectMapper);

    }

    protected Map<String, Object> createTemplateFor(String indexWildcard, Map<String, Object> mapping) {
        return ImmutableMap.of(
                "template", indexWildcard,
                "mappings", mapping
        );
    }

    @BeforeEach
    public void setUp() {
        //noinspection UnstableApiUsage
        eventBus = new EventBus("indices-test");
        final Node node = new Node(createNodeAdapter());
        final IndexMappingFactory indexMappingFactory = new IndexMappingFactory(node,
                ImmutableMap.of(MessageIndexTemplateProvider.MESSAGE_TEMPLATE_TYPE, new MessageIndexTemplateProvider()));
        indices = new Indices(
                indexMappingFactory,
                mock(NodeId.class),
                new NullAuditEventSender(),
                eventBus,
                indicesAdapter()
        );
    }

    @AfterEach
    public void cleanUp() {
        indicesToCleanUp.forEach(client()::deleteIndices);
        indicesToCleanUp.clear();
    }

    protected String createRandomIndex(final String prefix) {
        final String index = client().createRandomIndex(prefix);
        indicesToCleanUp.add(index);
        return index;
    }

    @ContainerMatrixTest
    public void testGetIndicesBlocksStatus() {
        final String index = createRandomIndex("indices_it_");

        IndicesBlockStatus indicesBlocksStatus = indices.getIndicesBlocksStatus(Collections.singletonList(index));
        assertEquals(0, indicesBlocksStatus.countBlockedIndices());

        client().setIndexBlock(index);
        indicesBlocksStatus = indices.getIndicesBlocksStatus(Collections.singletonList(index));
        assertEquals(1, indicesBlocksStatus.countBlockedIndices());
        final Collection<String> indexBlocks = indicesBlocksStatus.getIndexBlocks(index);
        assertEquals(1, indexBlocks.size());
        assertTrue(indexBlocks.contains("index.blocks.read_only_allow_delete"));

        client().resetIndexBlock(index);
        indicesBlocksStatus = indices.getIndicesBlocksStatus(Collections.singletonList(index));
        assertEquals(0, indicesBlocksStatus.countBlockedIndices());
    }

    @ContainerMatrixTest
    public void testDelete() {
        final String index = createRandomIndex("indices_it_");
        indices.delete(index);

        assertThat(client().indicesExists(index)).isFalse();
    }

    @ContainerMatrixTest
    public void testClose() {
        final String index = createRandomIndex("indices_it_");

        assertThat(indices.isOpen(index)).isTrue();

        indices.close(index);

        assertThat(indices.isClosed(index)).isTrue();
    }

    @ContainerMatrixTest
    public void findClosedIndices() {
        final String index1 = createRandomIndex("indices_it_");
        client().closeIndex(index1);
        final String index2 = createRandomIndex("otherindices_it_");
        client().closeIndex(index2);
        client().createRandomIndex("evenmoreindices_it_");

        final Set<String> closedIndices = indices.getClosedIndices(Collections.singleton("*"));

        assertThat(closedIndices).containsExactlyInAnyOrder(index1, index2);
    }

    @ContainerMatrixTest
    public void aliasExistsReturnsIfGivenIndexNameIsIndexOrAlias() {
        final String index = createRandomIndex("indices_it_");
        final String alias = "graylog_alias_exists";
        assertThat(indices.aliasExists(alias)).isFalse();

        client().addAliasMapping(index, alias);

        assertThat(indices.aliasExists(alias)).isTrue();
        assertThat(indices.exists(alias)).isFalse();
    }

    @ContainerMatrixTest
    public void aliasExistsReturnsIfGivenIndexHasAlias() {
        final String indexName = createRandomIndex("indices_it_");

        assertThat(indices.aliasExists(indexName)).isFalse();
    }

    @ContainerMatrixTest
    public void existsIndicatesPresenceOfGivenIndex() {
        final String indexName = createRandomIndex("indices_it_");

        assertThat(indices.exists(indexName)).isTrue();
    }

    @ContainerMatrixTest
    public void existsReturnsFalseIfGivenIndexDoesNotExists() {
        final String indexNotAlias = "graylog_index_does_not_exist";
        assertThat(indices.exists(indexNotAlias)).isFalse();
    }

    @ContainerMatrixTest
    public void aliasTargetReturnsListOfTargetsGivenAliasIsPointingTo() {
        final String index = createRandomIndex("indices_it_");
        final String alias = "graylog_alias_target";
        assertThat(indices.aliasTarget(alias)).isEmpty();

        client().addAliasMapping(index, alias);

        assertThat(indices.aliasTarget(alias)).contains(index);
    }

    @ContainerMatrixTest
    public void indexRangeStatsOfIndexReturnsMinMaxTimestampsForGivenIndex() {
        importFixture("org/graylog2/indexer/indices/IndicesIT.json");

        IndexRangeStats stats = indices.indexRangeStatsOfIndex(INDEX_NAME);

        assertThat(stats.min()).isEqualTo(new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC));
    }

    @ContainerMatrixTest
    public void indexRangeStatsWorksForEmptyIndex() {
        final String indexName = createRandomIndex("indices_it_");

        IndexRangeStats stats = indices.indexRangeStatsOfIndex(indexName);

        assertThat(stats.min()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
    }

    @ContainerMatrixTest
    public void indexRangeStatsThrowsExceptionIfIndexIsClosed() {
        assertThrows(IndexNotFoundException.class, () -> {
            final String index = createRandomIndex("indices_it_");

            client().closeIndex(index);

            indices.indexRangeStatsOfIndex(index);
        });
    }

    @ContainerMatrixTest
    public void indexRangeStatsThrowsExceptionIfIndexDoesNotExists() {
        assertThrows(IndexNotFoundException.class, () -> {
            indices.indexRangeStatsOfIndex("does-not-exist");
        });
    }

    @ContainerMatrixTest
    public void createEnsuresIndexTemplateExists() {
        final String indexName = "index_template_test";
        indicesToCleanUp.add(indexName);

        final String templateName = indexSetConfig.indexTemplateName();

        assertThat(client().templateExists(templateName)).isFalse();

        indices.create(indexName, indexSet);

        assertThat(client().templateExists(templateName)).isTrue();
        assertThat(client().fieldType(indexName, "message")).isEqualTo("text");
    }

    @ContainerMatrixTest
    public void createOverwritesIndexTemplate() {
        final String indexName = "index_template_test";
        indicesToCleanUp.add(indexName);

        final String templateName = indexSetConfig.indexTemplateName();

        final Map<String, Object> beforeMapping = ImmutableMap.of(
                "_source", ImmutableMap.of("enabled", false),
                "properties", ImmutableMap.of("message",
                        ImmutableMap.of("type", "text")));

        final Map<String, Object> templateSource = createTemplateFor(indexSet.getIndexWildcard(), beforeMapping);

        client().putTemplate(templateName, templateSource);

        indices.create(indexName, indexSet);

        assertThat(client().fieldType(indexName, "message")).isEqualTo("text");
    }

    @ContainerMatrixTest
    public void indexCreationDateReturnsIndexCreationDateOfExistingIndexAsDateTime() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final String indexName = createRandomIndex("indices_it_");

        final Optional<DateTime> indexCreationDate = indices.indexCreationDate(indexName);
        assertThat(indexCreationDate).isNotEmpty()
                .hasValueSatisfying(date -> assertThat(date.toDate()).isCloseTo(now.toDate(), TimeUnit.SECONDS.toMillis(1)));
    }

    @ContainerMatrixTest
    public void indexCreationDateReturnsEmptyOptionalForNonExistingIndex() {
        assertThat(indices.indexCreationDate("index_missing")).isEmpty();
    }

    @ContainerMatrixTest
    public void closePostsIndicesClosedEvent() {
        final org.graylog2.indexer.indices.IndicesIT.IndicesEventListener listener = new org.graylog2.indexer.indices.IndicesIT.IndicesEventListener();
        eventBus.register(listener);

        final String index = createRandomIndex("indices_it_");

        indices.close(index);

        assertThat(listener.indicesClosedEvents).containsOnly(IndicesClosedEvent.create(index));
        assertThat(listener.indicesDeletedEvents).isEmpty();
        assertThat(listener.indicesReopenedEvents).isEmpty();
    }

    @ContainerMatrixTest
    public void deletePostsIndicesDeletedEvent() {
        final org.graylog2.indexer.indices.IndicesIT.IndicesEventListener listener = new org.graylog2.indexer.indices.IndicesIT.IndicesEventListener();
        eventBus.register(listener);

        final String index = createRandomIndex("indices_it_");

        indices.delete(index);

        assertThat(listener.indicesDeletedEvents).containsOnly(IndicesDeletedEvent.create(index));
        assertThat(listener.indicesClosedEvents).isEmpty();
        assertThat(listener.indicesReopenedEvents).isEmpty();
    }

    @ContainerMatrixTest
    public void reopenIndexPostsIndicesReopenedEvent() {
        final org.graylog2.indexer.indices.IndicesIT.IndicesEventListener listener = new org.graylog2.indexer.indices.IndicesIT.IndicesEventListener();
        eventBus.register(listener);

        final String index = createRandomIndex("indices_it_");

        client().closeIndex(index);

        indices.reopenIndex(index);

        assertThat(listener.indicesReopenedEvents).containsOnly(IndicesReopenedEvent.create(index));
        assertThat(listener.indicesClosedEvents).isEmpty();
        assertThat(listener.indicesDeletedEvents).isEmpty();
    }

    @ContainerMatrixTest
    public void ensureIndexTemplateDoesntThrowOnIgnoreIndexTemplateAndExistingTemplate() {
        final String templateName = indexSetConfig.indexTemplateName();

        indices.ensureIndexTemplate(indexSet);

        assertThat(client().templateExists(templateName)).isTrue();

        indices = new Indices(
                createThrowingIndexMappingFactory(indexSetConfig),
                mock(NodeId.class),
                new NullAuditEventSender(),
                eventBus,
                indicesAdapter());

        assertThatCode(() -> indices.ensureIndexTemplate(indexSet)).doesNotThrowAnyException();

        assertThat(client().templateExists(templateName)).isTrue();
    }

    private IndexMappingFactory createThrowingIndexMappingFactory(IndexSetConfig indexSetConfig) {
        final IndexMappingFactory indexMappingFactory = mock(IndexMappingFactory.class);
        when(indexMappingFactory.createIndexMapping(any()))
                .thenThrow(new IgnoreIndexTemplate(true, "Reason",
                        indexSetConfig.indexPrefix(), indexSetConfig.indexTemplateName(),
                        indexSetConfig.indexTemplateType().orElse(null)));
        return indexMappingFactory;
    }

    @ContainerMatrixTest
    public void ensureIndexTemplateThrowsOnIgnoreIndexTemplateAndNonExistingTemplate() {
        final String templateName = indexSetConfig.indexTemplateName();

        try {
            indices.deleteIndexTemplate(indexSet);
        } catch (Exception e) {
        }

        assertThat(client().templateExists(templateName)).isFalse();

        indices = new Indices(
                createThrowingIndexMappingFactory(indexSetConfig),
                mock(NodeId.class),
                new NullAuditEventSender(),
                eventBus,
                indicesAdapter());

        assertThatCode(() -> indices.ensureIndexTemplate(indexSet))
                .isExactlyInstanceOf(IndexTemplateNotFoundException.class)
                .hasMessage("No index template with name 'template-1' (type - 'null') found in Elasticsearch");
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

    @ContainerMatrixTest
    public void getIndices() {
        final IndexSet indexSet = new TestIndexSet(indexSetConfig.toBuilder().indexPrefix("indices_it").build());
        final String index1 = createRandomIndex("indices_it_");
        final String index2 = createRandomIndex("indices_it_");

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

    @ContainerMatrixTest
    public void testIndexId() {
        final String index = createRandomIndex("indices_it_");
        String uuid = indices.getIndexId(index);
        assertThat(uuid).isNotEmpty();
        assert (Base64.isBase64(uuid));
    }

    @ContainerMatrixTest
    public void storeSizeInBytesReturnsValue() {
        final String index = createRandomIndex("foo");

        final Optional<Long> storeSizeInBytes = indices.getStoreSizeInBytes(index);

        assertThat(storeSizeInBytes).isNotEmpty();
    }

    @ContainerMatrixTest
    public void retrievesCreationTimeOfIndexInUTC() {
        final String index = createRandomIndex("foo");

        final Optional<DateTime> creationDate = indices.indexCreationDate(index);

        assertThat(creationDate).hasValueSatisfying(dt ->
                assertThat(dt.getZone()).isEqualTo(DateTimeZone.UTC));
    }

    @ContainerMatrixTest
    public void retrievesAllAliasesForIndex() {
        final String index1 = createRandomIndex("foo-");
        final String index2 = createRandomIndex("foo-");

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

    @ContainerMatrixTest
    public void retrieveIndexStatisticsForIndices() {
        final String index = createRandomIndex("indices_it_");

        final Set<IndexStatistics> indicesStats = indices.getIndicesStats(Collections.singleton(index));

        assertThat(indicesStats).isNotEmpty();
    }

    @ContainerMatrixTest
    public void cyclingDeflectorMovesAliasFromOldToNewTarget() {
        final String deflector = "indices_it_deflector";

        final String index1 = createRandomIndex("indices_it_");
        final String index2 = createRandomIndex("indices_it_");

        client().addAliasMapping(index1, deflector);

        assertThat(indices.aliasTarget(deflector)).hasValue(index1);

        indices.cycleAlias(deflector, index2, index1);

        assertThat(indices.aliasTarget(deflector)).hasValue(index2);
    }

    @ContainerMatrixTest
    public void retrievingIndexStatsForWildcard() {
        final IndexSetStatsCreator indexSetStatsCreator = new IndexSetStatsCreator(indices);
        final String indexPrefix = "indices_wildcard_";
        final String wildcard = indexPrefix + "*";
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getIndexWildcard()).thenReturn(wildcard);

        createRandomIndex(indexPrefix);
        createRandomIndex(indexPrefix);

        final IndexSetStats indexSetStats = indexSetStatsCreator.getForIndexSet(indexSet);

        assertThat(indexSetStats.indices()).isEqualTo(2L);
        assertThat(indexSetStats.size()).isNotZero();
    }

    @ContainerMatrixTest
    public void waitForRedIndexReturnsStatus() {
        final HealthStatus healthStatus = indices.waitForRecovery("this_index_does_not_exist", 0);

        assertThat(healthStatus).isEqualTo(HealthStatus.Red);
    }

    @ContainerMatrixTest
    public void numberOfMessagesReturnsCorrectSize() {
        importFixture("org/graylog2/indexer/indices/IndicesIT.json");

        assertThat(indices.numberOfMessages("graylog_0")).isEqualTo(10);
    }

    @ContainerMatrixTest
    public void optimizeIndexJobDoesNotThrowException() {
        importFixture("org/graylog2/indexer/indices/IndicesIT.json");

        indices.optimizeIndex("graylog_0", 1, Duration.minutes(1));
    }

    @ContainerMatrixTest
    public void aliasTargetReturnsListOfTargetsGivenAliasIsPointingToWithWildcards() {
        final String index = createRandomIndex("indices_it_");
        final String alias = "graylog_alias_target";
        assertThat(indices.aliasTarget(alias)).isEmpty();

        client().addAliasMapping(index, alias);

        assertThat(indices.aliasTarget("graylog_alias_*")).contains(index);
    }

    @ContainerMatrixTest
    public void aliasTargetSupportsIndicesWithPlusInName() {
        final String prefixWithPlus = "index+set_";
        final String index = createRandomIndex(prefixWithPlus);
        final String alias = prefixWithPlus + "deflector";
        assertThat(indices.aliasTarget(alias)).isEmpty();

        client().addAliasMapping(index, alias);

        assertThat(indices.aliasTarget(prefixWithPlus + "*")).contains(index);
    }

    @ContainerMatrixTest
    public void removeAliasesRemovesSecondTarget() {
        final String randomIndices = "random_";
        final String index = createRandomIndex(randomIndices);
        final String index2 = createRandomIndex(randomIndices);
        final String alias = randomIndices + "deflector";
        assertThat(indices.aliasTarget(alias)).isEmpty();

        client().addAliasMapping(index, alias);
        client().addAliasMapping(index2, alias);

        assertThatThrownBy(() -> indices.aliasTarget(alias))
                .isInstanceOf(TooManyAliasesException.class);

        indices.removeAliases(alias, Collections.singleton(index));

        assertThat(indices.aliasTarget(alias)).contains(index2);
    }

    // Prevent accidental use of AliasActions.Type.REMOVE_INDEX,
    // as despite being an *Alias* Action, it actually deletes an index!
    @ContainerMatrixTest
    public void cyclingAliasLeavesOldIndexInPlace() {
        final String deflector = "indices_it_deflector";

        final String index1 = createRandomIndex("indices_it_");
        final String index2 = createRandomIndex("indices_it_");

        client().addAliasMapping(index1, deflector);

        indices.cycleAlias(deflector, index2, index1);

        assertThat(indices.exists(index1)).isTrue();
    }
}
