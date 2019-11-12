package org.graylog.plugins.views.search.elasticsearch.searchtypes;

import com.google.common.collect.ImmutableSet;
import edu.emory.mathcs.backport.java.util.Collections;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.graylog.events.event.EventDto;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorator;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.events.EventSummary;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;

public class ESEventList implements ESSearchTypeHandler<EventList> {

    @Inject
    public ESEventList() {
    }

    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, EventList eventList, ESGeneratedQueryContext queryContext) {
        queryContext.searchSourceBuilder(eventList)
                .size(10000);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SearchType.Result doExtractResult(SearchJob job, Query query, EventList searchType, SearchResult result, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Set<String> effectiveStreams = searchType.streams().isEmpty() ? query.usedStreamIds() : searchType.streams();
        final List<EventSummary> eventSummaries = result.getHits(Map.class, false).stream()
                .map(hit -> EventSummary.parse((Map<String, Object>) hit.source))
                .filter(eventSummary -> effectiveStreams.containsAll(eventSummary.streams()))
                .collect(Collectors.toList());
        return EventList.Result.builder()
                .events(eventSummaries)
                .id(searchType.id())
                .build();
    }
}
