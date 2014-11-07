/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.Tools;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class IndexHelper {

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

    public static FilterBuilder getTimestampRangeFilter(TimeRange range) throws InvalidRangeFormatException {
        if (range == null) {
            return null;
        }

        return FilterBuilders.rangeFilter("timestamp")
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
                                                       Deflector deflector,
                                                       TimeRange range) {
        Set<String> indices = Sets.newHashSet();

        for (IndexRange indexRange : indexRangeService.getFrom((int) (range.getFrom().getMillis() / 1000))) {
            indices.add(indexRange.getIndexName());
        }

        // Always include the most recent index in some cases.
        if (indices.isEmpty() || range instanceof RelativeRange) {
            indices.add(deflector.getCurrentActualTargetIndex());
        }

        return indices;
    }

    public static Set<IndexRange> determineAffectedIndicesWithRanges(IndexRangeService indexRangeService,
                                                                     Deflector deflector,
                                                                     TimeRange range) {
        Set<IndexRange> indices = Sets.newTreeSet(new Comparator<IndexRange>() {
            @Override
            public int compare(IndexRange o1, IndexRange o2) {
                return o2.getStart().compareTo(o1.getStart());
            }
        });

        for (IndexRange indexRange : indexRangeService.getFrom((int) (range.getFrom().getMillis() / 1000))) {
            indices.add(indexRange);
        }

        // Always include the most recent index in some cases.
        if (indices.isEmpty() || range instanceof RelativeRange) {
            try {
                final IndexRange deflectorIndexRange = indexRangeService.get(deflector.getCurrentActualTargetIndex());
                indices.add(deflectorIndexRange);
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }

        return indices;
    }

    public static class InvalidRangeFormatException extends Exception {
    }
}
