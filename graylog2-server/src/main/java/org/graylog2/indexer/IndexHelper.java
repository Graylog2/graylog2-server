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
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IndexHelper {
    public static Set<String> getOldestIndices(IndexSet indexSet, int count) {
        final String[] managedIndicesNames = indexSet.getManagedIndices();
        if (count <= 0 || managedIndicesNames.length <= count) {
            return Collections.emptySet();
        }

        final List<Integer> numbers = new ArrayList<>(managedIndicesNames.length);
        for (String indexName : managedIndicesNames) {
            indexSet.extractIndexNumber(indexName).ifPresent(numbers::add);
        }

        final List<String> sorted = prependPrefixes(indexSet.getIndexPrefix(), Tools.asSortedSet(numbers));

        return ImmutableSet.copyOf(sorted.subList(0, count));
    }

    @Nullable
    public static QueryBuilder getTimestampRangeFilter(TimeRange range) {
        if (range == null) {
            return null;
        }

        return QueryBuilders.rangeQuery(Message.FIELD_TIMESTAMP)
                .gte(Tools.buildElasticSearchTimeFormat(range.getFrom()))
                .lte(Tools.buildElasticSearchTimeFormat(range.getTo()));
    }

    private static List<String> prependPrefixes(String prefix, Collection<Integer> numbers) {
        final List<String> r = new ArrayList<>();
        for (int number : numbers) {
            r.add(prefix + "_" + number);
        }

        return r;
    }
}
