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
package org.graylog2.indexer.searches;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.buffers.processors.fakestreams.FakeStream;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeComparator;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.ranges.MongoIndexRange;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.searches.ScrollCommand.NO_BATCHSIZE;
import static org.joda.time.DateTimeZone.UTC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class SearchesIT extends ElasticsearchBaseTest {
    private static final String REQUEST_TIMER_NAME = "org.graylog2.indexer.searches.Searches.elasticsearch.requests";
    private static final String RANGES_HISTOGRAM_NAME = "org.graylog2.indexer.searches.Searches.elasticsearch.ranges";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final String INDEX_NAME = "graylog_0";
    private static final String STREAM_ID = "000000000000000000000001";
    private static final ImmutableSortedSet<IndexRange> INDEX_RANGES = ImmutableSortedSet.orderedBy(new IndexRangeComparator())
            .add(new IndexRange() {
                @Override
                public String indexName() {
                    return INDEX_NAME;
                }

                @Override
                public DateTime calculatedAt() {
                    return DateTime.now(UTC);
                }

                @Override
                public DateTime end() {
                    return new DateTime(2015, 1, 1, 0, 0, UTC);
                }

                @Override
                public int calculationDuration() {
                    return 0;
                }

                @Override
                public List<String> streamIds() {
                    return Collections.singletonList(STREAM_ID);
                }

                @Override
                public DateTime begin() {
                    return new DateTime(0L, UTC);
                }
            }).build();

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

    @Mock
    protected IndexRangeService indexRangeService;

    @Mock
    protected StreamService streamService;

    @Mock
    protected Indices indices;

    @Mock
    protected IndexSetRegistry indexSetRegistry;

    protected MetricRegistry metricRegistry;
    protected Searches searches;

    @Before
    public void setUp() throws Exception {
        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(INDEX_RANGES);
        when(indices.getAllMessageFieldsForIndices(any(String[].class))).thenReturn(ImmutableMap.of(INDEX_NAME, Collections.singleton("n")));
        metricRegistry = new MetricRegistry();
        this.searches = createSearches();
    }

    public abstract Searches createSearches();

    @Test
    public void testCountWithoutFilter() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        CountResult result = searches.count("*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)));

        assertThat(result.count()).isEqualTo(10L);
    }

    @Test
    public void testCountWithFilter() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        final IndexSetConfig indexSetConfig = IndexSetConfig.builder()
                .id("id")
                .title("title")
                .indexPrefix("prefix")
                .shards(1)
                .replicas(0)
                .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
                .creationDate(ZonedDateTime.of(2017, 5, 24, 0, 0, 0, 0, ZoneOffset.UTC))
                .indexAnalyzer("standard")
                .indexTemplateName("template")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();
        final IndexSet indexSet = new TestIndexSet(indexSetConfig);
        final Stream stream = new FakeStream("test") {
            @Override
            public IndexSet getIndexSet() {
                return indexSet;
            }
        };
        when(streamService.load(STREAM_ID)).thenReturn(stream);
        CountResult result = searches.count("*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)),
                "streams:" + STREAM_ID);

        assertThat(result.count()).isEqualTo(5L);
    }

    @Test
    public void testCountWithInvalidFilter() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        CountResult result = searches.count("*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)),
                "foobar-not-a-filter");

        assertThat(result.count()).isEqualTo(0L);
    }

    @Test
    public void countRecordsMetrics() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        CountResult result = searches.count("*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)));

        assertThat(metricRegistry.getTimers()).containsKey(REQUEST_TIMER_NAME);
        assertThat(metricRegistry.getHistograms()).containsKey(RANGES_HISTOGRAM_NAME);

        Timer timer = metricRegistry.timer(REQUEST_TIMER_NAME);
        assertThat(timer.getCount()).isEqualTo(1L);
    }

    @Test
    public void testFieldStats() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        FieldStatsResult fieldStats = searches.fieldStats("n", "*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)));

        assertThat(fieldStats).satisfies(result -> {
            assertThat(result.searchHits()).hasSize(10);
            assertThat(result.count()).isEqualTo(8);
            assertThat(result.min()).isEqualTo(1.0);
            assertThat(result.max()).isEqualTo(4.0);
            assertThat(result.mean()).isEqualTo(2.375);
            assertThat(result.sum()).isEqualTo(19.0);
            assertThat(result.sumOfSquares()).isEqualTo(53.0);
            assertThat(result.variance()).isEqualTo(0.984375);
            assertThat(result.stdDeviation()).isEqualTo(0.9921567416492215);
        });
    }

    @Test
    public void fieldStatsRecordsMetrics() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        FieldStatsResult result = searches.fieldStats("n", "*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)));

        assertThat(metricRegistry.getTimers()).containsKey(REQUEST_TIMER_NAME);
        assertThat(metricRegistry.getHistograms()).containsKey(RANGES_HISTOGRAM_NAME);

        Timer timer = metricRegistry.timer(REQUEST_TIMER_NAME);
        assertThat(timer.getCount()).isEqualTo(1L);

        Histogram histogram = metricRegistry.histogram(RANGES_HISTOGRAM_NAME);
        assertThat(histogram.getCount()).isEqualTo(1L);
        assertThat(histogram.getSnapshot().getValues()).containsExactly(86400L);
    }

    @Test
    public void determineAffectedIndicesWithRangesIncludesDeflectorTarget() throws Exception {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final MongoIndexRange indexRange0 = MongoIndexRange.create("graylog_0", now, now.plusDays(1), now, 0);
        final MongoIndexRange indexRange1 = MongoIndexRange.create("graylog_1", now.plusDays(1), now.plusDays(2), now, 0);
        final MongoIndexRange indexRangeLatest = MongoIndexRange.create("graylog_2", new DateTime(0L, DateTimeZone.UTC), new DateTime(0L, DateTimeZone.UTC), now, 0);
        final SortedSet<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR)
                .add(indexRange0)
                .add(indexRange1)
                .add(indexRangeLatest)
                .build();

        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indices);

        final TimeRange absoluteRange = AbsoluteRange.create(now.minusDays(1), now.plusDays(1));
        final TimeRange keywordRange = KeywordRange.create("1 day ago");
        final TimeRange relativeRange = RelativeRange.create(3600);

        assertThat(searches.determineAffectedIndicesWithRanges(absoluteRange, null))
                .containsExactly(indexRangeLatest, indexRange0, indexRange1);
        assertThat(searches.determineAffectedIndicesWithRanges(keywordRange, null))
                .containsExactly(indexRangeLatest, indexRange0, indexRange1);
        assertThat(searches.determineAffectedIndicesWithRanges(relativeRange, null))
                .containsExactly(indexRangeLatest, indexRange0, indexRange1);
    }

    @Test
    public void determineAffectedIndicesWithRangesDoesNotIncludesDeflectorTargetIfMissing() throws Exception {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final MongoIndexRange indexRange0 = MongoIndexRange.create("graylog_0", now, now.plusDays(1), now, 0);
        final MongoIndexRange indexRange1 = MongoIndexRange.create("graylog_1", now.plusDays(1), now.plusDays(2), now, 0);
        final SortedSet<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR)
                .add(indexRange0)
                .add(indexRange1)
                .build();

        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indices);

        final TimeRange absoluteRange = AbsoluteRange.create(now.minusDays(1), now.plusDays(1));
        final TimeRange keywordRange = KeywordRange.create("1 day ago");
        final TimeRange relativeRange = RelativeRange.create(3600);

        assertThat(searches.determineAffectedIndicesWithRanges(absoluteRange, null))
                .containsExactly(indexRange0, indexRange1);
        assertThat(searches.determineAffectedIndicesWithRanges(keywordRange, null))
                .containsExactly(indexRange0, indexRange1);
        assertThat(searches.determineAffectedIndicesWithRanges(relativeRange, null))
                .containsExactly(indexRange0, indexRange1);
    }

    @Test
    public void determineAffectedIndicesWithRangesExcludeEvents() throws Exception {
        final Set<IndexSet> eventIndexSets = Arrays.asList("gl-events", "gl-system-events").stream().
                map(prefix -> new TestIndexSet(indexSet.getConfig().toBuilder()
                        .indexPrefix(prefix)
                        .indexTemplateType(IndexSetConfig.TemplateType.EVENTS)
                        .build())).collect(Collectors.toSet());
        when(indexSetRegistry.getForIndices(anyCollection())).thenReturn(eventIndexSets);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final MongoIndexRange indexRange0 = MongoIndexRange.create("gl-events_0", now, now.plusDays(1), now, 0);
        final MongoIndexRange indexRange1 = MongoIndexRange.create("gl-system-events_2", now.plusDays(1), now.plusDays(2), now, 0);
        final MongoIndexRange indexRange2 = MongoIndexRange.create("graylog_0", now, now.plusDays(1), now, 0);
        final SortedSet<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR)
                .add(indexRange0)
                .add(indexRange1)
                .add(indexRange2)
                .build();

        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indices);

        final TimeRange absoluteRange = AbsoluteRange.create(now.minusDays(1), now.plusDays(1));

        assertThat(searches.determineAffectedIndicesWithRanges(absoluteRange, null))
                .containsExactly(indexRange2);
    }

    @Test
    public void determineAffectedIndicesIncludesDeflectorTarget() throws Exception {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final MongoIndexRange indexRange0 = MongoIndexRange.create("graylog_0", now, now.plusDays(1), now, 0);
        final MongoIndexRange indexRange1 = MongoIndexRange.create("graylog_1", now.plusDays(1), now.plusDays(2), now, 0);
        final MongoIndexRange indexRangeLatest = MongoIndexRange.create("graylog_2", new DateTime(0L, DateTimeZone.UTC), new DateTime(0L, DateTimeZone.UTC), now, 0);
        final SortedSet<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR)
                .add(indexRange0)
                .add(indexRange1)
                .add(indexRangeLatest)
                .build();

        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indices);

        final TimeRange absoluteRange = AbsoluteRange.create(now.minusDays(1), now.plusDays(1));
        final TimeRange keywordRange = KeywordRange.create("1 day ago");
        final TimeRange relativeRange = RelativeRange.create(3600);

        assertThat(searches.determineAffectedIndices(absoluteRange, null))
                .containsExactlyInAnyOrder(indexRangeLatest.indexName(), indexRange0.indexName(), indexRange1.indexName());
        assertThat(searches.determineAffectedIndices(keywordRange, null))
                .containsExactlyInAnyOrder(indexRangeLatest.indexName(), indexRange0.indexName(), indexRange1.indexName());
        assertThat(searches.determineAffectedIndices(relativeRange, null))
                .containsExactlyInAnyOrder(indexRangeLatest.indexName(), indexRange0.indexName(), indexRange1.indexName());
    }

    @Test
    public void determineAffectedIndicesDoesNotIncludesDeflectorTargetIfMissing() throws Exception {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final MongoIndexRange indexRange0 = MongoIndexRange.create("graylog_0", now, now.plusDays(1), now, 0);
        final MongoIndexRange indexRange1 = MongoIndexRange.create("graylog_1", now.plusDays(1), now.plusDays(2), now, 0);
        final SortedSet<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR)
                .add(indexRange0)
                .add(indexRange1)
                .build();

        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indices);

        final TimeRange absoluteRange = AbsoluteRange.create(now.minusDays(1), now.plusDays(1));
        final TimeRange keywordRange = KeywordRange.create("1 day ago");
        final TimeRange relativeRange = RelativeRange.create(3600);

        assertThat(searches.determineAffectedIndices(absoluteRange, null))
                .containsOnly(indexRange0.indexName(), indexRange1.indexName());
        assertThat(searches.determineAffectedIndices(keywordRange, null))
                .containsOnly(indexRange0.indexName(), indexRange1.indexName());
        assertThat(searches.determineAffectedIndices(relativeRange, null))
                .containsOnly(indexRange0.indexName(), indexRange1.indexName());
    }

    @Test
    public void determineAffectedIndicesFilterIndexPrefix() throws Exception {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final MongoIndexRange indexRange0 = MongoIndexRange.create("graylog_0", now, now.plusDays(1), now, 0);
        final MongoIndexRange indexRange1 = MongoIndexRange.create("graylog_1", now.plusDays(1), now.plusDays(2), now, 0);
        final MongoIndexRange b0 = MongoIndexRange.create("b_0", now.plusDays(1), now.plusDays(2), now, 0);
        final MongoIndexRange b1 = MongoIndexRange.create("b_1", now.plusDays(1), now.plusDays(2), now, 0);
        final SortedSet<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR)
                .add(indexRange0)
                .add(indexRange1)
                .add(b0)
                .add(b1)
                .build();

        final Stream bStream = mock(Stream.class);

        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indices);
        when(streamService.load(eq("123456789ABCDEF"))).thenReturn(bStream);
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.isManagedIndex(startsWith("b_"))).thenReturn(true);
        when(bStream.getIndexSet()).thenReturn(indexSet);

        final TimeRange absoluteRange = AbsoluteRange.create(now.minusDays(1), now.plusDays(1));

        assertThat(searches.determineAffectedIndices(absoluteRange, "streams:123456789ABCDEF"))
                .containsOnly(b0.indexName(), b1.indexName());
    }

    @Test
    public void searchDoesNotIncludeJestMetadata() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final SearchResult searchResult = searches.search("_id:1", range, 0, 0, Sorting.DEFAULT);

        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getTotalResults()).isEqualTo(1L);
        assertThat(searchResult.getFields()).doesNotContain("es_metadata_id", "es_metadata_version");
    }

    @Test
    public void fieldStatsDoesNotIncludeJestMetadata() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final FieldStatsResult fieldStatsResult = searches.fieldStats("n", "_id:1", range);

        assertThat(fieldStatsResult).isNotNull();
        assertThat(fieldStatsResult.searchHits()).isNotNull();
        assertThat(fieldStatsResult.searchHits()).hasSize(1);
        final ResultMessage resultMessage = fieldStatsResult.searchHits().get(0);
        assertThat(resultMessage.getMessage().getFields()).doesNotContainKeys("es_metadata_id", "es_metadata_version");
    }

    @Test
    public void searchReturnsCorrectTotalHits() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final SearchResult searchResult = searches.search("*", range, 5, 0, Sorting.DEFAULT);

        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getResults()).hasSize(5);
        assertThat(searchResult.getTotalResults()).isEqualTo(10L);
        assertThat(searchResult.getFields()).doesNotContain("es_metadata_id", "es_metadata_version");
    }

    @Test
    public void searchReturnsResultWithSelectiveFields() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final SearchesConfig searchesConfig = SearchesConfig.builder()
                .query("*")
                .range(range)
                .limit(1)
                .offset(0)
                .fields(Collections.singletonList("source"))
                .build();
        final SearchResult searchResult = searches.search(searchesConfig);

        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getResults()).hasSize(1);
        assertThat(searchResult.getTotalResults()).isEqualTo(10L);
    }

    @Test
    public void scrollReturnsResultWithSelectiveFields() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        when(indexSetRegistry.getForIndices(Collections.singleton("graylog_0"))).thenReturn(Collections.singleton(indexSet));
        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final ScrollResult scrollResult = searches.scroll("*", range, 5, 0, Collections.singletonList("source"), null, NO_BATCHSIZE);

        assertThat(scrollResult).isNotNull();
        assertThat(scrollResult.getQueryHash()).isNotEmpty();
        assertThat(scrollResult.totalHits()).isEqualTo(10L);

        final ScrollResult.ScrollChunk firstChunk = scrollResult.nextChunk();
        assertThat(firstChunk).isNotNull();
        assertThat(firstChunk.getMessages()).hasSize(5);
        assertThat(firstChunk.isFirstChunk()).isTrue();
        assertThat(firstChunk.getFields()).containsExactly("source");
    }

    @Test
    public void scrollReturnsMultipleChunksRespectingBatchSize() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        when(indexSetRegistry.getForIndices(Collections.singleton("graylog_0"))).thenReturn(Collections.singleton(indexSet));
        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final ScrollResult scrollResult = searches.scroll("*", range, -1, 0, Collections.singletonList("source"), null, 2);

        assertThat(scrollResult).isNotNull();
        assertThat(scrollResult.totalHits()).isEqualTo(10L);

        ScrollResult.ScrollChunk scrollChunk = scrollResult.nextChunk();
        assertThat(scrollChunk.isFirstChunk()).isTrue();

        final Set<ResultMessage> resultMessages = new HashSet<>(5);
        while (scrollChunk != null && !scrollChunk.getMessages().isEmpty()) {
            assertThat(scrollChunk.getMessages()).hasSize(2);
            assertThat(scrollChunk.getFields()).containsExactly("source");

            resultMessages.addAll(scrollChunk.getMessages());
            scrollChunk = scrollResult.nextChunk();
        }

        assertThat(resultMessages).hasSize(10);
    }

    @Test
    public void scrollReturnsMultipleChunksRespectingLimit() throws Exception {
        importFixture("org/graylog2/indexer/searches/SearchesIT.json");

        when(indexSetRegistry.getForIndices(Collections.singleton("graylog_0"))).thenReturn(Collections.singleton(indexSet));
        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final ScrollResult scrollResult = searches.scroll("*", range, 5, 0, Collections.singletonList("source"), null, 2);

        assertThat(scrollResult).isNotNull();
        assertThat(scrollResult.totalHits()).isEqualTo(10L);

        ScrollResult.ScrollChunk scrollChunk = scrollResult.nextChunk();
        assertThat(scrollChunk.isFirstChunk()).isTrue();

        final Set<ResultMessage> resultMessages = new HashSet<>(5);
        while (scrollChunk != null && !scrollChunk.getMessages().isEmpty()) {
            resultMessages.addAll(scrollChunk.getMessages());
            scrollChunk = scrollResult.nextChunk();
        }

        assertThat(resultMessages).hasSize(5);
    }
}
