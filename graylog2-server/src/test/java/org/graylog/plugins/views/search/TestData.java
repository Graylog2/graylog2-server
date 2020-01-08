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
package org.graylog.plugins.views.search;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestData {
    public static Map<String, PluginMetadataSummary> requirementsMap(String... requirementNames) {
        final Map<String, PluginMetadataSummary> requirements = new HashMap<>();

        for (String req : requirementNames)
            requirements.put(req, PluginMetadataSummary.create("", req, "", URI.create("www.affenmann.info"), "6.6.6", ""));

        return requirements;
    }

    public static Search searchWithQueriesWithStreams(String... queriesWithStreams) {
        Set<Query> queries = Arrays.stream(queriesWithStreams).map(TestData::queryWithStreams).collect(Collectors.toSet());

        return Search.builder().queries(ImmutableSet.copyOf(queries)).build();
    }

    public static ImmutableSet<Query> queriesWithSearchTypes(String... queriesWithSearchTypes) {
        Set<Query> queries = Arrays.stream(queriesWithSearchTypes)
                .map(TestData::queryWithSearchTypes).collect(Collectors.toSet());
        return ImmutableSet.copyOf(queries);
    }

    public static Query queryWithSearchTypes(String searchTypeIds) {
        String[] split = searchTypeIds.isEmpty() ? new String[0] : searchTypeIds.split(",");
        return validQueryBuilder().searchTypes(searchTypes(split)).build();
    }

    public static Set<SearchType> searchTypes(String... ids) {
        return Arrays.stream(ids).map(TestData::messageList).collect(Collectors.toSet());
    }

    public static MessageList messageList(String id) {
        return MessageList.builder().id(id).build();
    }

    public static Query queryWithStreams(String streamIds) {
        String[] split = streamIds.isEmpty() ? new String[0] : streamIds.split(",");
        return queryWithStreams(split);
    }

    public static Query queryWithStreams(String... streamIds) {
        Query.Builder builder = validQueryBuilder();

        if (streamIds.length > 0)
            builder = builder.filter(StreamFilter.anyIdOf(streamIds));

        return builder.build();
    }

    public static Query.Builder validQueryBuilder() {
        return Query.builder().id(UUID.randomUUID().toString()).timerange(mock(TimeRange.class)).query(new BackendQuery.Fallback());
    }

    public static SearchType searchTypeWithStreams(String streamIds) {
        final SearchType searchType = mock(SearchType.class);
        final Set<String> streamIdSet = streamIds.isEmpty() ? Collections.emptySet() : new HashSet<>(Arrays.asList(streamIds.split(",")));
        when(searchType.effectiveStreams()).thenReturn(streamIdSet);
        when(searchType.id()).thenReturn(UUID.randomUUID().toString());

        return searchType;
    }
}
