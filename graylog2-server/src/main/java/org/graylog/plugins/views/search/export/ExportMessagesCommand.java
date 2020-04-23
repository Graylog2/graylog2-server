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
package org.graylog.plugins.views.search.export;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;

import java.util.LinkedHashSet;
import java.util.OptionalInt;
import java.util.Set;

import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;

@AutoValue
public abstract class ExportMessagesCommand {

    //add decorators

    public static final AbsoluteRange DEFAULT_TIME_RANGE = lastFiveMinutes();
    public static final ElasticsearchQueryString DEFAULT_QUERY = ElasticsearchQueryString.empty();
    public static final Set<String> DEFAULT_STREAMS = ImmutableSet.of();
    public static final LinkedHashSet<String> DEFAULT_FIELDS = linkedHashSetOf("timestamp", "source", "message");
    public static final LinkedHashSet<Sort> DEFAULT_SORT = linkedHashSetOf(Sort.create("timestamp", SortOrder.DESC));
    public static final int DEFAULT_CHUNK_SIZE = 1000;

    public static AbsoluteRange lastFiveMinutes() {
        try {
            RelativeRange relativeRange = RelativeRange.create(300);
            return AbsoluteRange.create(relativeRange.getFrom(), relativeRange.getTo());
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException("Error creating default time range", e);
        }
    }

    public abstract AbsoluteRange timeRange();

    public abstract ElasticsearchQueryString queryString();

    public abstract Set<String> streams();

    public abstract LinkedHashSet<String> fieldsInOrder();

    public abstract LinkedHashSet<Sort> sort();

    public abstract int chunkSize();

    public abstract OptionalInt limit();

    public static ExportMessagesCommand withDefaults() {
        return builder().build();
    }

    public static ExportMessagesCommand.Builder builder() {
        return ExportMessagesCommand.Builder.create();
    }

    public abstract ExportMessagesCommand.Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract ExportMessagesCommand.Builder timeRange(AbsoluteRange timeRange);

        public abstract ExportMessagesCommand.Builder streams(Set<String> streams);

        public abstract ExportMessagesCommand.Builder queryString(ElasticsearchQueryString queryString);

        public abstract ExportMessagesCommand.Builder fieldsInOrder(LinkedHashSet<String> fieldsInOrder);

        public ExportMessagesCommand.Builder fieldsInOrder(String... fieldsInOrder) {
            return fieldsInOrder(linkedHashSetOf(fieldsInOrder));
        }

        public abstract ExportMessagesCommand.Builder sort(LinkedHashSet<Sort> sort);

        public abstract ExportMessagesCommand.Builder chunkSize(int chunkSize);

        public abstract ExportMessagesCommand.Builder limit(Integer limit);

        abstract ExportMessagesCommand autoBuild();

        public ExportMessagesCommand build() {
            return autoBuild();
        }

        public static ExportMessagesCommand.Builder create() {
            return new AutoValue_ExportMessagesCommand.Builder()
                    .timeRange(DEFAULT_TIME_RANGE)
                    .streams(DEFAULT_STREAMS)
                    .queryString(DEFAULT_QUERY)
                    .fieldsInOrder(DEFAULT_FIELDS)
                    .sort(DEFAULT_SORT)
                    .chunkSize(DEFAULT_CHUNK_SIZE);
        }
    }
}
