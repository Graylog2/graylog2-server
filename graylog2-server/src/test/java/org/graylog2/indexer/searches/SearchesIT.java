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
package org.graylog2.indexer.searches;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.Configuration;
import org.graylog2.buffers.processors.fakestreams.FakeStream;
import org.graylog2.indexer.IndexHelper;
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
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.results.TermsStatsResult;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.Tools;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchesIT extends ElasticsearchBaseTest {
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
    private IndexRangeService indexRangeService;

    @Mock
    private StreamService streamService;

    @Mock
    private Indices indices;

    @Mock
    private IndexSetRegistry indexSetRegistry;

    private MetricRegistry metricRegistry;
    private Searches searches;

    @Before
    public void setUp() throws Exception {
        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(INDEX_RANGES);
        when(indices.getAllMessageFieldsForIndices(any(String[].class))).thenReturn(ImmutableMap.of(INDEX_NAME, Collections.singleton("n")));
        metricRegistry = new MetricRegistry();
        searches = new Searches(new Configuration(), indexRangeService, metricRegistry, streamService, indices, indexSetRegistry, jestClient(), new ScrollResult.Factory() {
            @Override
            public ScrollResult create(io.searchbox.core.SearchResult initialResult, String query, List<String> fields) {
                return new ScrollResult(jestClient(), new ObjectMapper(), initialResult, query, fields);
            }
            @Override
            public ScrollResult create(io.searchbox.core.SearchResult initialResult, String query, String scroll, List<String> fields) {
                return new ScrollResult(jestClient(), new ObjectMapper(), initialResult, query, scroll, fields);
            }
        }, Duration.minutes(1));
    }

    @Test
    public void testCountWithoutFilter() throws Exception {
        importFixture("SearchesIT.json");

        CountResult result = searches.count("*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)));

        assertThat(result.count()).isEqualTo(10L);
    }

    @Test
    public void testCountWithFilter() throws Exception {
        importFixture("SearchesIT.json");

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
        importFixture("SearchesIT.json");

        CountResult result = searches.count("*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)),
                "foobar-not-a-filter");

        assertThat(result.count()).isEqualTo(0L);
    }

    @Test
    public void countRecordsMetrics() throws Exception {
        importFixture("SearchesIT.json");

        CountResult result = searches.count("*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)));

        assertThat(metricRegistry.getTimers()).containsKey(REQUEST_TIMER_NAME);
        assertThat(metricRegistry.getHistograms()).containsKey(RANGES_HISTOGRAM_NAME);

        Timer timer = metricRegistry.timer(REQUEST_TIMER_NAME);
        assertThat(timer.getCount()).isEqualTo(1L);
    }

    @Test
    public void testTerms() throws Exception {
        importFixture("SearchesIT.json");

        TermsResult result = searches.terms("n", 25, "*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)));

        assertThat(result.getTotal()).isEqualTo(10L);
        assertThat(result.getMissing()).isEqualTo(2L);
        assertThat(result.getTerms())
                .hasSize(4)
                .containsEntry("1", 2L)
                .containsEntry("2", 2L)
                .containsEntry("3", 3L)
                .containsEntry("4", 1L);
    }

    @Test
    public void testTermsWithNonExistingIndex() throws Exception {
        importFixture("SearchesIT.json");

        final SortedSet<IndexRange> indexRanges = ImmutableSortedSet
                .orderedBy(IndexRange.COMPARATOR)
                .add(MongoIndexRange.create(INDEX_NAME,
                        new DateTime(0L, UTC),
                        new DateTime(2015, 1, 1, 0, 0, UTC),
                        DateTime.now(UTC),
                        0,
                        null))
                .add(MongoIndexRange.create("does-not-exist",
                        new DateTime(0L, UTC),
                        new DateTime(2015, 1, 1, 0, 0, UTC),
                        DateTime.now(UTC),
                        0,
                        null))
                .build();
        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indexRanges);

        TermsResult result = searches.terms("n", 25, "*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)));

        assertThat(result.getTotal()).isEqualTo(10L);
        assertThat(result.getMissing()).isEqualTo(2L);
        assertThat(result.getTerms())
                .hasSize(4)
                .containsEntry("1", 2L)
                .containsEntry("2", 2L)
                .containsEntry("3", 3L)
                .containsEntry("4", 1L);
    }

    @Test
    public void termsRecordsMetrics() throws Exception {
        importFixture("SearchesIT.json");

        TermsResult result = searches.terms("n", 25, "*", AbsoluteRange.create(
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
    public void testTermsAscending() throws Exception {
        importFixture("SearchesIT.json");

        TermsResult result = searches.terms("n", 1, "*", null, AbsoluteRange.create(
            new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
            new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)), Sorting.Direction.ASC);

        assertThat(result.getTotal()).isEqualTo(10L);
        assertThat(result.getMissing()).isEqualTo(2L);
        assertThat(result.getTerms())
            .hasSize(1)
            .containsEntry("4", 1L);
    }

    @Test
    public void testTermsStats() throws Exception {
        importFixture("SearchesIT-terms_stats.json");

        TermsStatsResult r = searches.termsStats("f", "n", Searches.TermsStatsOrder.COUNT, 25, "*",
                AbsoluteRange.create(
                        new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                        new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC))
        );

        assertThat(r.getResults()).hasSize(2);
        assertThat(r.getResults().get(0))
                .hasSize(7)
                .containsEntry("key_field", "Ho");
    }

    @Test
    public void termsStatsRecordsMetrics() throws Exception {
        importFixture("SearchesIT-terms_stats.json");

        TermsStatsResult r = searches.termsStats("f", "n", Searches.TermsStatsOrder.COUNT, 25, "*",
                AbsoluteRange.create(
                        new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                        new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC))
        );

        assertThat(metricRegistry.getTimers()).containsKey(REQUEST_TIMER_NAME);
        assertThat(metricRegistry.getHistograms()).containsKey(RANGES_HISTOGRAM_NAME);

        Timer timer = metricRegistry.timer(REQUEST_TIMER_NAME);
        assertThat(timer.getCount()).isEqualTo(1L);

        Histogram histogram = metricRegistry.histogram(RANGES_HISTOGRAM_NAME);
        assertThat(histogram.getCount()).isEqualTo(1L);
        assertThat(histogram.getSnapshot().getValues()).containsExactly(86400L);
    }

    @Test
    public void testFieldStats() throws Exception {
        importFixture("SearchesIT.json");

        FieldStatsResult result = searches.fieldStats("n", "*", AbsoluteRange.create(
                new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC)));

        assertThat(result.getSearchHits()).hasSize(10);
        assertThat(result.getCount()).isEqualTo(8);
        assertThat(result.getMin()).isEqualTo(1.0);
        assertThat(result.getMax()).isEqualTo(4.0);
        assertThat(result.getMean()).isEqualTo(2.375);
        assertThat(result.getSum()).isEqualTo(19.0);
        assertThat(result.getSumOfSquares()).isEqualTo(53.0);
        assertThat(result.getVariance()).isEqualTo(0.984375);
        assertThat(result.getStdDeviation()).isEqualTo(0.9921567416492215);
    }

    @Test
    public void fieldStatsRecordsMetrics() throws Exception {
        importFixture("SearchesIT.json");

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
    @SuppressWarnings("unchecked")
    public void testHistogram() throws Exception {
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        HistogramResult h = searches.histogram("*", Searches.DateHistogramInterval.HOUR, range);

        assertThat(h.getInterval()).isEqualTo(Searches.DateHistogramInterval.HOUR);
        assertThat(h.getHistogramBoundaries()).isEqualTo(range);
        assertThat(h.getResults())
                .hasSize(5)
                .containsEntry(new DateTime(2015, 1, 1, 1, 0, UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 2, 0, UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 3, 0, UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 4, 0, UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 5, 0, UTC).getMillis() / 1000L, 2L);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testHistogramWithNonExistingIndex() throws Exception {
        importFixture("SearchesIT.json");

        final SortedSet<IndexRange> indexRanges = ImmutableSortedSet
                .orderedBy(IndexRange.COMPARATOR)
                .add(MongoIndexRange.create(INDEX_NAME,
                        new DateTime(0L, UTC),
                        new DateTime(2015, 1, 1, 0, 0, UTC),
                        DateTime.now(UTC),
                        0,
                        null))
                .add(MongoIndexRange.create("does-not-exist",
                        new DateTime(0L, UTC),
                        new DateTime(2015, 1, 1, 0, 0, UTC),
                        DateTime.now(UTC),
                        0,
                        null))
                .build();
        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indexRanges);

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        HistogramResult h = searches.histogram("*", Searches.DateHistogramInterval.HOUR, range);

        assertThat(h.getInterval()).isEqualTo(Searches.DateHistogramInterval.HOUR);
        assertThat(h.getHistogramBoundaries()).isEqualTo(range);
        assertThat(h.getResults())
                .hasSize(5)
                .containsEntry(new DateTime(2015, 1, 1, 1, 0, UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 2, 0, UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 3, 0, UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 4, 0, UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 5, 0, UTC).getMillis() / 1000L, 2L);
    }

    @Test
    public void histogramRecordsMetrics() throws Exception {
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC));
        HistogramResult h = searches.histogram("*", Searches.DateHistogramInterval.MINUTE, range);

        assertThat(metricRegistry.getTimers()).containsKey(REQUEST_TIMER_NAME);
        assertThat(metricRegistry.getHistograms()).containsKey(RANGES_HISTOGRAM_NAME);

        Timer timer = metricRegistry.timer(REQUEST_TIMER_NAME);
        assertThat(timer.getCount()).isEqualTo(1L);

        Histogram histogram = metricRegistry.histogram(RANGES_HISTOGRAM_NAME);
        assertThat(histogram.getCount()).isEqualTo(1L);
        assertThat(histogram.getSnapshot().getValues()).containsExactly(86400L);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFieldHistogram() throws Exception {
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        HistogramResult h = searches.fieldHistogram("*", "n", Searches.DateHistogramInterval.HOUR, null, range, false);

        assertThat(h.getInterval()).isEqualTo(Searches.DateHistogramInterval.HOUR);
        assertThat(h.getHistogramBoundaries()).isEqualTo(range);
        assertThat(h.getResults()).hasSize(5);
        assertThat((Map<String, Number>) h.getResults().get(new DateTime(2015, 1, 1, 1, 0, UTC).getMillis() / 1000L))
                .containsEntry("total_count", 2L)
                .containsEntry("total", 0.0);
        assertThat((Map<String, Number>) h.getResults().get(new DateTime(2015, 1, 1, 2, 0, UTC).getMillis() / 1000L))
                .containsEntry("total_count", 2L)
                .containsEntry("total", 4.0)
                .containsEntry("mean", 2.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFieldHistogramWithMonth() throws Exception {
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        HistogramResult h = searches.fieldHistogram("*", "n", Searches.DateHistogramInterval.MONTH, null, range, false);

        assertThat(h.getInterval()).isEqualTo(Searches.DateHistogramInterval.MONTH);
        assertThat(h.getHistogramBoundaries()).isEqualTo(range);
        assertThat(h.getResults()).hasSize(1);
        assertThat((Map<String, Number>) h.getResults().get(new DateTime(2015, 1, 1, 0, 0, UTC).getMillis() / 1000L))
                .containsEntry("total_count", 10L)
                .containsEntry("total", 19.0)
                .containsEntry("min", 1.0)
                .containsEntry("max", 4.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFieldHistogramWithQuarter() throws Exception {
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        HistogramResult h = searches.fieldHistogram("*", "n", Searches.DateHistogramInterval.QUARTER, null, range, false);

        assertThat(h.getInterval()).isEqualTo(Searches.DateHistogramInterval.QUARTER);
        assertThat(h.getHistogramBoundaries()).isEqualTo(range);
        assertThat(h.getResults()).hasSize(1);
        assertThat((Map<String, Number>) h.getResults().get(new DateTime(2015, 1, 1, 0, 0, UTC).getMillis() / 1000L))
                .containsEntry("total_count", 10L)
                .containsEntry("total", 19.0)
                .containsEntry("min", 1.0)
                .containsEntry("max", 4.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFieldHistogramWithYear() throws Exception {
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        HistogramResult h = searches.fieldHistogram("*", "n", Searches.DateHistogramInterval.YEAR, null, range, false);

        assertThat(h.getInterval()).isEqualTo(Searches.DateHistogramInterval.YEAR);
        assertThat(h.getHistogramBoundaries()).isEqualTo(range);
        assertThat(h.getResults()).hasSize(1);
        assertThat((Map<String, Number>) h.getResults().get(new DateTime(2015, 1, 1, 0, 0, UTC).getMillis() / 1000L))
                .containsEntry("total_count", 10L)
                .containsEntry("total", 19.0)
                .containsEntry("min", 1.0)
                .containsEntry("max", 4.0);
    }

    @Test
    public void fieldHistogramRecordsMetrics() throws Exception {
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC));
        HistogramResult h = searches.fieldHistogram("*", "n", Searches.DateHistogramInterval.MINUTE, null, range, false);

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
    public void getTimestampRangeFilterReturnsNullIfTimeRangeIsNull() {
        assertThat(IndexHelper.getTimestampRangeFilter(null)).isNull();
    }

    @Test
    public void getTimestampRangeFilterReturnsRangeQueryWithGivenTimeRange() {
        final DateTime from = new DateTime(2016, 1, 15, 12, 0, DateTimeZone.UTC);
        final DateTime to = from.plusHours(1);
        final TimeRange timeRange = AbsoluteRange.create(from, to);
        final RangeQueryBuilder queryBuilder = (RangeQueryBuilder) IndexHelper.getTimestampRangeFilter(timeRange);
        assertThat(queryBuilder).isNotNull();
        assertThat(queryBuilder.fieldName()).isEqualTo("timestamp");
        assertThat(queryBuilder.from()).isEqualTo(Tools.buildElasticSearchTimeFormat(from));
        assertThat(queryBuilder.to()).isEqualTo(Tools.buildElasticSearchTimeFormat(to));
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
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final SearchResult searchResult = searches.search("_id:1", range, 0, 0, Sorting.DEFAULT);

        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getTotalResults()).isEqualTo(1L);
        assertThat(searchResult.getFields()).doesNotContain("es_metadata_id", "es_metadata_version");
    }

    @Test
    public void fieldStatsDoesNotIncludeJestMetadata() throws Exception {
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final FieldStatsResult fieldStatsResult = searches.fieldStats("n", "_id:1", range);

        assertThat(fieldStatsResult).isNotNull();
        assertThat(fieldStatsResult.getSearchHits()).isNotNull();
        assertThat(fieldStatsResult.getSearchHits()).hasSize(1);
        final ResultMessage resultMessage = fieldStatsResult.getSearchHits().get(0);
        assertThat(resultMessage.getMessage().getFields()).doesNotContainKeys("es_metadata_id", "es_metadata_version");
    }

    @Test
    public void searchReturnsCorrectTotalHits() throws Exception {
        importFixture("SearchesIT.json");

        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final SearchResult searchResult = searches.search("*", range, 5, 0, Sorting.DEFAULT);

        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getResults()).hasSize(5);
        assertThat(searchResult.getTotalResults()).isEqualTo(10L);
        assertThat(searchResult.getFields()).doesNotContain("es_metadata_id", "es_metadata_version");
    }

    @Test
    public void searchReturnsResultWithSelectiveFields() throws Exception {
        importFixture("SearchesIT.json");

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
        importFixture("SearchesIT.json");

        when(indexSetRegistry.getForIndices(Collections.singleton("graylog_0"))).thenReturn(Collections.singleton(indexSet));
        final AbsoluteRange range = AbsoluteRange.create(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).withZone(UTC), new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC).withZone(UTC));
        final ScrollResult scrollResult = searches.scroll("*", range, 5, 0, Collections.singletonList("source"), null);

        assertThat(scrollResult).isNotNull();
        assertThat(scrollResult.getQueryHash()).isNotEmpty();
        assertThat(scrollResult.totalHits()).isEqualTo(10L);

        final ScrollResult.ScrollChunk firstChunk = scrollResult.nextChunk();
        assertThat(firstChunk).isNotNull();
        assertThat(firstChunk.getMessages()).hasSize(5);
        assertThat(firstChunk.isFirstChunk()).isTrue();
        assertThat(firstChunk.getFields()).containsExactly("source");
    }
}
