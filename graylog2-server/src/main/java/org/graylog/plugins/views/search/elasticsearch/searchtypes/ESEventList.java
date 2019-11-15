package org.graylog.plugins.views.search.elasticsearch.searchtypes;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.events.EventSummary;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ESEventList implements ESSearchTypeHandler<EventList> {
    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, EventList eventList,
                                    ESGeneratedQueryContext queryContext) {
        queryContext.searchSourceBuilder(eventList)
                .size(10000);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> extractResult(SearchResult result) {
        return result.getHits(Map.class, false).stream()
                .map(hit -> (Map<String, Object>) hit.source)
                .collect(Collectors.toList());
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, EventList searchType, SearchResult result,
                                             MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Set<String> effectiveStreams = searchType.streams().isEmpty()
                ? query.usedStreamIds()
                : searchType.streams();
        final List<EventSummary> eventSummaries = extractResult(result).stream()
                .map(EventSummary::parse)
                .filter(eventSummary -> effectiveStreams.containsAll(eventSummary.streams()))
                .collect(Collectors.toList());
        return EventList.Result.builder()
                .events(eventSummaries)
                .id(searchType.id())
                .build();
    }
}
