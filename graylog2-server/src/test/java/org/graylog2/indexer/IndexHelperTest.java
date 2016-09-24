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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.ranges.MongoIndexRange;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class IndexHelperTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IndexRangeService indexRangeService;

    @BeforeClass
    public static void initialize() {
        DateTimeUtils.setCurrentMillisFixed(new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC).getMillis());
    }

    @AfterClass
    public static void shutdown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testGetOldestIndices() {
        final Set<String> indices = ImmutableSet.<String>builder()
            .add("graylog2_production_1")
            .add("graylog2_production_7")
            .add("graylog2_production_0")
            .add("graylog2_production_2")
            .add("graylog2_production_4")
            .add("graylog2_production_6")
            .add("graylog2_production_3")
            .add("graylog2_production_5")
            .add("graylog2_production_8")
            .add("graylog2_production_9")
            .add("graylog2_production_10")
            .add("graylog2_production_110")
            .add("graylog2_production_125")
            .add("graylog2_production_20")
            .add("graylog2_production_21")
            .build();

        assertThat(IndexHelper.getOldestIndices(indices, 7)).containsOnly(
            "graylog2_production_0",
            "graylog2_production_1",
            "graylog2_production_2",
            "graylog2_production_3",
            "graylog2_production_4",
            "graylog2_production_5",
            "graylog2_production_6");
        assertThat(IndexHelper.getOldestIndices(indices, 1)).containsOnly("graylog2_production_0");
    }

    @Test
    public void testGetOldestIndicesWithEmptySetAndTooHighOffset() {
        assertThat(IndexHelper.getOldestIndices(Collections.<String>emptySet(), 9001)).isEmpty();
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
        when(indexRangeService.get("graylog_2")).thenReturn(indexRangeLatest);

        final TimeRange absoluteRange = AbsoluteRange.create(now.minusDays(1), now.plusDays(1));
        final TimeRange keywordRange = KeywordRange.create("1 day ago");
        final TimeRange relativeRange = RelativeRange.create(3600);

        assertThat(IndexHelper.determineAffectedIndicesWithRanges(indexRangeService, absoluteRange))
            .containsExactly(indexRangeLatest, indexRange0, indexRange1);
        assertThat(IndexHelper.determineAffectedIndicesWithRanges(indexRangeService, keywordRange))
            .containsExactly(indexRangeLatest, indexRange0, indexRange1);
        assertThat(IndexHelper.determineAffectedIndicesWithRanges(indexRangeService, relativeRange))
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

        assertThat(IndexHelper.determineAffectedIndicesWithRanges(indexRangeService, absoluteRange))
            .containsExactly(indexRange0, indexRange1);
        assertThat(IndexHelper.determineAffectedIndicesWithRanges(indexRangeService, keywordRange))
            .containsExactly(indexRange0, indexRange1);
        assertThat(IndexHelper.determineAffectedIndicesWithRanges(indexRangeService, relativeRange))
            .containsExactly(indexRange0, indexRange1);
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
        when(indexRangeService.get("graylog_2")).thenReturn(indexRangeLatest);

        final TimeRange absoluteRange = AbsoluteRange.create(now.minusDays(1), now.plusDays(1));
        final TimeRange keywordRange = KeywordRange.create("1 day ago");
        final TimeRange relativeRange = RelativeRange.create(3600);

        assertThat(IndexHelper.determineAffectedIndices(indexRangeService, absoluteRange))
            .containsExactly(indexRangeLatest.indexName(), indexRange0.indexName(), indexRange1.indexName());
        assertThat(IndexHelper.determineAffectedIndices(indexRangeService, keywordRange))
            .containsExactly(indexRangeLatest.indexName(), indexRange0.indexName(), indexRange1.indexName());
        assertThat(IndexHelper.determineAffectedIndices(indexRangeService, relativeRange))
            .containsExactly(indexRangeLatest.indexName(), indexRange0.indexName(), indexRange1.indexName());
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

        assertThat(IndexHelper.determineAffectedIndices(indexRangeService, absoluteRange))
            .containsOnly(indexRange0.indexName(), indexRange1.indexName());
        assertThat(IndexHelper.determineAffectedIndices(indexRangeService, keywordRange))
            .containsOnly(indexRange0.indexName(), indexRange1.indexName());
        assertThat(IndexHelper.determineAffectedIndices(indexRangeService, relativeRange))
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
        assertThat(queryBuilder)
            .isNotNull()
            .hasFieldOrPropertyWithValue("name", "timestamp")
            .hasFieldOrPropertyWithValue("from", Tools.buildElasticSearchTimeFormat(from))
            .hasFieldOrPropertyWithValue("to", Tools.buildElasticSearchTimeFormat(to));
    }
}
