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

import com.google.common.collect.ImmutableSet;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class Defaults {
    public static final TimeRange DEFAULT_TIME_RANGE = lastFiveMinutes();
    public static final BackendQuery DEFAULT_QUERY = ElasticsearchQueryString.empty();
    public static final Set<String> DEFAULT_STREAMS = ImmutableSet.of(); //TODO: use all permitted streams
    public static final Set<String> DEFAULT_FIELDS = ImmutableSet.of("timestamp", "source", "message");
    public static final Set<Sort> DEFAULT_SORT = ImmutableSet.of(Sort.create("_doc", SortOrder.ASC));

    private static RelativeRange lastFiveMinutes() {
        try {
            return RelativeRange.create(300);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException("Error creating default time range", e);
        }
    }

    public MessagesRequest fillInIfNecessary(MessagesRequest request) {
        MessagesRequest.Builder builder = request.toBuilder();

        fill(request.timeRange(), builder::timeRange, DEFAULT_TIME_RANGE);
        fill(request.queryString(), builder::queryString, DEFAULT_QUERY);
        fill(request.streams(), builder::streams, DEFAULT_STREAMS);
        fill(request.fieldsInOrder(), builder::fieldsInOrder, DEFAULT_FIELDS);
        fill(request.sort(), builder::sort, DEFAULT_SORT);

        return builder.build();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T> void fill(Optional<T> field, Consumer<T> builderMethod, T defaultValue) {
        if (!field.isPresent())
            builderMethod.accept(defaultValue);
    }
}
