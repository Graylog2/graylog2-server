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
import com.google.common.collect.ImmutableSortedSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule;
import com.lordofthejars.nosqlunit.elasticsearch.EmbeddedElasticsearch;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.graylog2.Configuration;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.nosqlunit.IndexCreatingLoadStrategyFactory;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeComparator;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.results.TermsStatsResult;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;

import static com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.lordofthejars.nosqlunit.elasticsearch.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchesTest {
    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule().build();
    private static final String REQUEST_TIMER_NAME = "org.graylog2.indexer.searches.Searches.elasticsearch.requests";
    private static final String RANGES_HISTOGRAM_NAME = "org.graylog2.indexer.searches.Searches.elasticsearch.ranges";

    @Rule
    public ElasticsearchRule elasticsearchRule;

    private static final String INDEX_NAME = "graylog";
    private static final SortedSet<IndexRange> INDEX_RANGES = ImmutableSortedSet
            .orderedBy(new IndexRangeComparator())
            .add(new IndexRange() {
                @Override
                public String indexName() {
                    return INDEX_NAME;
                }

                @Override
                public DateTime calculatedAt() {
                    return DateTime.now();
                }

                @Override
                public DateTime end() {
                    return new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
                }

                @Override
                public int calculationDuration() {
                    return 0;
                }

                @Override
                public DateTime begin() {
                    return new DateTime(0L, DateTimeZone.UTC);
                }
            }).build();

    @Mock
    private Deflector deflector;
    @Mock
    private IndexRangeService indexRangeService;

    private MetricRegistry metricRegistry;
    private Searches searches;

    @Inject
    private Client client;

    public SearchesTest() {
        this.elasticsearchRule = newElasticsearchRule().defaultEmbeddedElasticsearch();
        this.elasticsearchRule.setLoadStrategyFactory(new IndexCreatingLoadStrategyFactory(Collections.singleton(INDEX_NAME)));
    }

    @Before
    public void setUp() throws Exception {
        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(INDEX_RANGES);
        metricRegistry = new MetricRegistry();
        searches = new Searches(new Configuration(), deflector, indexRangeService, client, metricRegistry);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testCount() throws Exception {
        CountResult result = searches.count("*", new AbsoluteRange(
                new DateTime(2015, 1, 1, 0, 0),
                new DateTime(2015, 1, 2, 0, 0)));

        assertThat(result.getCount()).isEqualTo(10L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void countRecordsMetrics() throws Exception {
        CountResult result = searches.count("*", new AbsoluteRange(
                new DateTime(2015, 1, 1, 0, 0),
                new DateTime(2015, 1, 2, 0, 0)));

        assertThat(metricRegistry.getTimers()).containsKey(REQUEST_TIMER_NAME);
        assertThat(metricRegistry.getHistograms()).containsKey(RANGES_HISTOGRAM_NAME);

        Timer timer = metricRegistry.timer(REQUEST_TIMER_NAME);
        assertThat(timer.getCount()).isEqualTo(1L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testTerms() throws Exception {
        TermsResult result = searches.terms("n", 25, "*", new AbsoluteRange(
                new DateTime(2015, 1, 1, 0, 0),
                new DateTime(2015, 1, 2, 0, 0)));

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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void termsRecordsMetrics() throws Exception {
        TermsResult result = searches.terms("n", 25, "*", new AbsoluteRange(
                new DateTime(2015, 1, 1, 0, 0),
                new DateTime(2015, 1, 2, 0, 0)));

        assertThat(metricRegistry.getTimers()).containsKey(REQUEST_TIMER_NAME);
        assertThat(metricRegistry.getHistograms()).containsKey(RANGES_HISTOGRAM_NAME);

        Timer timer = metricRegistry.timer(REQUEST_TIMER_NAME);
        assertThat(timer.getCount()).isEqualTo(1L);

        Histogram histogram = metricRegistry.histogram(RANGES_HISTOGRAM_NAME);
        assertThat(histogram.getCount()).isEqualTo(1L);
        assertThat(histogram.getSnapshot().getValues()).containsExactly(86400L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testTermsStats() throws Exception {
        TermsStatsResult r = searches.termsStats("message", "n", Searches.TermsStatsOrder.COUNT, 25, "*",
                new AbsoluteRange(
                        new DateTime(2015, 1, 1, 0, 0),
                        new DateTime(2015, 1, 2, 0, 0))
        );

        assertThat(r.getResults()).hasSize(2);
        assertThat((Map<String, Object>) r.getResults().get(0))
                .hasSize(7)
                .containsEntry("key_field", "ho");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void termsStatsRecordsMetrics() throws Exception {
        TermsStatsResult r = searches.termsStats("message", "n", Searches.TermsStatsOrder.COUNT, 25, "*",
                new AbsoluteRange(
                        new DateTime(2015, 1, 1, 0, 0),
                        new DateTime(2015, 1, 2, 0, 0))
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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFieldStats() throws Exception {
        FieldStatsResult result = searches.fieldStats("n", "*", new AbsoluteRange(
                new DateTime(2015, 1, 1, 0, 0),
                new DateTime(2015, 1, 2, 0, 0)));

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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void fieldStatsRecordsMetrics() throws Exception {
        FieldStatsResult result = searches.fieldStats("n", "*", new AbsoluteRange(
                new DateTime(2015, 1, 1, 0, 0),
                new DateTime(2015, 1, 2, 0, 0)));

        assertThat(metricRegistry.getTimers()).containsKey(REQUEST_TIMER_NAME);
        assertThat(metricRegistry.getHistograms()).containsKey(RANGES_HISTOGRAM_NAME);

        Timer timer = metricRegistry.timer(REQUEST_TIMER_NAME);
        assertThat(timer.getCount()).isEqualTo(1L);

        Histogram histogram = metricRegistry.histogram(RANGES_HISTOGRAM_NAME);
        assertThat(histogram.getCount()).isEqualTo(1L);
        assertThat(histogram.getSnapshot().getValues()).containsExactly(86400L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    @SuppressWarnings("unchecked")
    public void testHistogram() throws Exception {
        final AbsoluteRange range = new AbsoluteRange(new DateTime(2015, 1, 1, 0, 0), new DateTime(2015, 1, 2, 0, 0));
        HistogramResult h = searches.histogram("*", Searches.DateHistogramInterval.MINUTE, range);

        assertThat(h.getInterval()).isEqualTo(Searches.DateHistogramInterval.MINUTE);
        assertThat(h.getHistogramBoundaries()).isEqualTo(range);
        assertThat(h.getResults())
                .hasSize(5)
                .containsEntry(new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 2, 0, DateTimeZone.UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 3, 0, DateTimeZone.UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 4, 0, DateTimeZone.UTC).getMillis() / 1000L, 2L)
                .containsEntry(new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC).getMillis() / 1000L, 2L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    @SuppressWarnings("unchecked")
    public void histogramRecordsMetrics() throws Exception {
        final AbsoluteRange range = new AbsoluteRange(new DateTime(2015, 1, 1, 0, 0), new DateTime(2015, 1, 2, 0, 0));
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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    @SuppressWarnings("unchecked")
    public void testFieldHistogram() throws Exception {
        final AbsoluteRange range = new AbsoluteRange(new DateTime(2015, 1, 1, 0, 0), new DateTime(2015, 1, 2, 0, 0));
        HistogramResult h = searches.fieldHistogram("*", "n", Searches.DateHistogramInterval.MINUTE, null, range);

        assertThat(h.getInterval()).isEqualTo(Searches.DateHistogramInterval.MINUTE);
        assertThat(h.getHistogramBoundaries()).isEqualTo(range);
        assertThat(h.getResults()).hasSize(5);
        assertThat((Map<String, Number>) h.getResults().get(new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC).getMillis() / 1000L))
                .containsEntry("total_count", 2L)
                .containsEntry("total", 0.0);
        assertThat((Map<String, Number>) h.getResults().get(new DateTime(2015, 1, 1, 2, 0, DateTimeZone.UTC).getMillis() / 1000L))
                .containsEntry("total_count", 2L)
                .containsEntry("total", 4.0)
                .containsEntry("mean", 2.0);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    @SuppressWarnings("unchecked")
    public void fieldHistogramRecordsMetrics() throws Exception {
        final AbsoluteRange range = new AbsoluteRange(new DateTime(2015, 1, 1, 0, 0), new DateTime(2015, 1, 2, 0, 0));
        HistogramResult h = searches.fieldHistogram("*", "n", Searches.DateHistogramInterval.MINUTE, null, range);

        assertThat(metricRegistry.getTimers()).containsKey(REQUEST_TIMER_NAME);
        assertThat(metricRegistry.getHistograms()).containsKey(RANGES_HISTOGRAM_NAME);

        Timer timer = metricRegistry.timer(REQUEST_TIMER_NAME);
        assertThat(timer.getCount()).isEqualTo(1L);

        Histogram histogram = metricRegistry.histogram(RANGES_HISTOGRAM_NAME);
        assertThat(histogram.getCount()).isEqualTo(1L);
        assertThat(histogram.getSnapshot().getValues()).containsExactly(86400L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFirstOfIndex() throws Exception {
        SearchHit searchHit = searches.firstOfIndex("graylog");

        assertThat(searchHit.getSource()).containsKey("timestamp");
        assertThat(searchHit.getSource().get("timestamp")).isEqualTo("2015-01-01 05:00:00.000");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testLastOfIndex() throws Exception {
        SearchHit searchHit = searches.lastOfIndex("graylog");

        assertThat(searchHit.getSource()).containsKey("timestamp");
        assertThat(searchHit.getSource().get("timestamp")).isEqualTo("2015-01-01 01:00:00.000");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindNewestMessageTimestampOfIndex() throws Exception {
        DateTime dateTime = searches.findNewestMessageTimestampOfIndex("graylog");

        assertThat(dateTime).isEqualTo(new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC));
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest-EmptyIndex.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindNewestMessageTimestampOfIndexWithEmptyIndex() throws Exception {
        DateTime dateTime = searches.findNewestMessageTimestampOfIndex("graylog");

        assertThat(dateTime).isNull();
    }

    @Test(expected = IndexMissingException.class)
    public void testFindNewestMessageTimestampOfIndexWithNonExistingIndex() throws Exception {
        searches.findNewestMessageTimestampOfIndex("does-not-exist");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindOldestMessageTimestampOfIndex() throws Exception {
        DateTime dateTime = searches.findOldestMessageTimestampOfIndex("graylog");

        assertThat(dateTime).isEqualTo(new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC));
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest-EmptyIndex.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindOldestMessageTimestampOfIndexWithEmptyIndex() throws Exception {
        DateTime dateTime = searches.findOldestMessageTimestampOfIndex("graylog");

        assertThat(dateTime).isNull();
    }

    @Test(expected = IndexMissingException.class)
    public void testFindOldestMessageTimestampOfIndexWithNonExistingIndex() throws Exception {
        searches.findOldestMessageTimestampOfIndex("does-not-exist");
    }
}
