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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class IndexHelper {
    private static final Logger LOG = LoggerFactory.getLogger(IndexHelper.class);

    public static Set<String> getOldestIndices(Set<String> indexNames, int count) {
        Set<String> r = Sets.newHashSet();

        if (count < 0 || indexNames.size() <= count) {
            return r;
        }

        Set<Integer> numbers = Sets.newHashSet();

        for (String indexName : indexNames) {
            numbers.add(Deflector.extractIndexNumber(indexName));
        }

        List<String> sorted = prependPrefixes(getPrefix(indexNames), Tools.asSortedList(numbers));

        // Add last x entries to return set.
        r.addAll(sorted.subList(0, count));

        return r;
    }

    public static RangeQueryBuilder decorateWithTimeZone(RangeQueryBuilder builder, TimeRange range) {
        // If both timestamps are in the same zone, we add the time_zone parameter to the filter. This allows us to
        // use non-UTC timestamps and Elasticsearch will convert them to UTC internally.
        // See: https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-range-query.html#_time_zone_in_range_queries
        if (range.getFrom().getZone().equals(range.getTo().getZone())) {
            return builder.timeZone(range.getFrom().getZone().toString());
        } else {
            return builder;
        }
    }

    public static DateHistogramBuilder decorateWithTimeZone(DateHistogramBuilder builder, TimeRange range) {
        // If both timestamps are in the same zone, we add the time_zone parameter to the filter. This allows us to
        // use non-UTC timestamps and Elasticsearch will convert them to UTC internally.
        // See: https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-range-query.html#_time_zone_in_range_queries
        if (range.getFrom().getZone().equals(range.getTo().getZone())) {
            return builder.timeZone(range.getFrom().getZone().toString());
        } else {
            return builder;
        }
    }

    public static QueryBuilder getTimestampRangeFilter(TimeRange range) throws InvalidRangeFormatException {
        if (range == null) {
            return null;
        }

        final RangeQueryBuilder builder = QueryBuilders.rangeQuery("timestamp")
                .gte(Tools.buildElasticSearchTimeFormatKeepZone(range.getFrom()))
                .lte(Tools.buildElasticSearchTimeFormatKeepZone(range.getTo()));

        return decorateWithTimeZone(builder, range);
    }

    private static String getPrefix(Set<String> names) {
        if (names.isEmpty()) {
            return "";
        }

        String name = (String) names.toArray()[0];
        return name.substring(0, name.lastIndexOf("_"));
    }

    private static List<String> prependPrefixes(String prefix, List<Integer> numbers) {
        List<String> r = Lists.newArrayList();

        for (int number : numbers) {
            r.add(prefix + "_" + number);
        }

        return r;
    }

    public static Set<String> determineAffectedIndices(IndexRangeService indexRangeService,
                                                       Deflector deflector,
                                                       TimeRange range) {
        final Set<IndexRange> indexRanges = determineAffectedIndicesWithRanges(indexRangeService, deflector, range);
        final ImmutableSet.Builder<String> indices = ImmutableSet.builder();
        for (IndexRange indexRange : indexRanges) {
            indices.add(indexRange.indexName());
        }

        return indices.build();
    }

    public static Set<IndexRange> determineAffectedIndicesWithRanges(IndexRangeService indexRangeService,
                                                                     Deflector deflector,
                                                                     TimeRange range) {
        final ImmutableSortedSet.Builder<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        for (IndexRange indexRange : indexRangeService.find(range.getFrom(), range.getTo())) {
            indices.add(indexRange);
        }

        // Always include the deflector target
        final String targetIndex = deflector.getCurrentActualTargetIndex();
        if (targetIndex != null) {
            try {
                final IndexRange deflectorIndexRange = indexRangeService.get(targetIndex);
                indices.add(deflectorIndexRange);
            } catch (NotFoundException e) {
                LOG.warn("Couldn't find latest deflector target index", e);
            }
        }

        return indices.build();
    }
}
