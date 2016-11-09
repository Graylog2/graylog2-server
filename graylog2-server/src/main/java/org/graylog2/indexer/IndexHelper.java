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
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IndexHelper {
    public static Set<String> getOldestIndices(IndexSet indexSet, Set<String> indexNames, int count) {
        if (count <= 0 || indexNames.size() <= count) {
            return Collections.emptySet();
        }

        final List<Integer> numbers = new ArrayList<>(indexNames.size());
        for (String indexName : indexNames) {
            indexSet.extractIndexNumber(indexName).ifPresent(numbers::add);
        }

        final List<String> sorted = prependPrefixes(getPrefix(indexNames), Tools.asSortedList(numbers));

        return Sets.newHashSet(sorted.subList(0, count));
    }

    @Nullable
    public static QueryBuilder getTimestampRangeFilter(TimeRange range) throws InvalidRangeFormatException {
        if (range == null) {
            return null;
        }

        return QueryBuilders.rangeQuery("timestamp")
                .gte(Tools.buildElasticSearchTimeFormat(range.getFrom()))
                .lte(Tools.buildElasticSearchTimeFormat(range.getTo()));
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
                                                       TimeRange range) {
        final Set<IndexRange> indexRanges = determineAffectedIndicesWithRanges(indexRangeService, range);
        final ImmutableSet.Builder<String> indices = ImmutableSet.builder();
        for (IndexRange indexRange : indexRanges) {
            indices.add(indexRange.indexName());
        }

        return indices.build();
    }

    public static Set<IndexRange> determineAffectedIndicesWithRanges(IndexRangeService indexRangeService,
                                                                     TimeRange range) {
        final ImmutableSortedSet.Builder<IndexRange> indices = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        for (IndexRange indexRange : indexRangeService.find(range.getFrom(), range.getTo())) {
            indices.add(indexRange);
        }

        return indices.build();
    }
}
