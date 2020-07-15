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
package org.graylog.storage.elasticsearch7.views.searchtypes;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.events.EventSummary;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ESEventList implements ESSearchTypeHandler<EventList> {
    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, EventList eventList,
                                    ESGeneratedQueryContext queryContext) {
        queryContext.searchSourceBuilder(eventList)
                .size(10000);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> extractResult(SearchResponse result) {
        return StreamSupport.stream(result.getHits().spliterator(), false)
                .map(SearchHit::getSourceAsMap)
                .collect(Collectors.toList());
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, EventList searchType, SearchResponse result,
                                             Aggregations aggregations, ESGeneratedQueryContext queryContext) {
        final Set<String> effectiveStreams = searchType.streams().isEmpty()
                ? query.usedStreamIds()
                : searchType.streams();
        final List<EventSummary> eventSummaries = extractResult(result).stream()
                .map(EventSummary::parse)
                .filter(eventSummary -> effectiveStreams.containsAll(eventSummary.streams()))
                .collect(Collectors.toList());
        final EventList.Result.Builder resultBuilder = EventList.Result.builder()
                .events(eventSummaries)
                .id(searchType.id());
        searchType.name().ifPresent(resultBuilder::name);
        return resultBuilder
                .build();
    }
}
