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
package org.graylog.plugins.views.search.elasticsearch.searchtypes.eventlist;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.graylog.events.event.EventDto;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESEventList;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.events.EventSummary;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;

import static org.assertj.core.api.Assertions.assertThat;

public class ESEventListTest {

    final private static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    final private static DateTime timestamp = formatter.parseDateTime("2019-03-30 14:00:00");

    private EventSummary eventSummaryGenerator(String id, Set<String> streams) {
        return EventSummary.builder()
                .id(id)
                .message("message")
                .streams(streams)
                .timestamp(DateTime.parse(timestamp.toString(Tools.ES_DATE_FORMAT_FORMATTER), Tools.ES_DATE_FORMAT_FORMATTER))
                .alert(false)
                .build();
    }

    static class TestESEventList extends ESEventList {
        private Map<String, Object> hitGenerator(String id, ArrayList<String> streams) {
            return ImmutableMap.of(
                    EventDto.FIELD_ID, id,
                    EventDto.FIELD_MESSAGE, "message",
                    EventDto.FIELD_SOURCE_STREAMS, streams,
                    EventDto.FIELD_EVENT_TIMESTAMP, timestamp.toString(Tools.ES_DATE_FORMAT_FORMATTER),
                    EventDto.FIELD_ALERT, false
            );
        }

        @Override
        protected List<Map<String, Object>> extractResult(SearchResult result) {
            final ArrayList<String> list1 = new ArrayList<>(); list1.add("stream-id-1");
            final ArrayList<String> list2 = new ArrayList<>(); list2.add("stream-id-2");
            final ArrayList<String> list3 = new ArrayList<>(); list3.add("stream-id-1"); list3.add("stream-id-2");
            final ArrayList<String> list4 = new ArrayList<>(); list4.add("stream-id-3");
            final ArrayList<String> list5 = new ArrayList<>(); list5.add("stream-id-1"); list5.add("stream-id-3");
            final ArrayList<String> list6 = new ArrayList<>(); list6.add("stream-id-2"); list6.add("stream-id-3");
            final ArrayList<String> list7 = new ArrayList<>(); list7.add("stream-id-1"); list7.add("stream-id-2");
            list7.add("stream-id-3");

            return ImmutableList.of(
                    hitGenerator("find-1", list1),
                    hitGenerator("find-2", list2),
                    hitGenerator("find-3", list3),
                    hitGenerator("do-not-find-1", list4),
                    hitGenerator("do-not-find-2", list5),
                    hitGenerator("do-not-find-3", list6),
                    hitGenerator("do-not-find-4", list7)
            );
        }
    }

    @Test
    public void testSortingOfStreamsInDoExtractResult() {
        final ESEventList esEventList = new TestESEventList();
        final SearchJob searchJob = mock(SearchJob.class);
        final Query query = mock(Query.class);
        final SearchResult searchResult = mock(SearchResult.class);
        final MetricAggregation metricAggregation = mock(MetricAggregation.class);
        final ESGeneratedQueryContext queryContext = mock(ESGeneratedQueryContext.class);

        final EventList eventList = EventList.builder()
                .id("search-type-id")
                .streams(ImmutableSet.of("stream-id-1", "stream-id-2"))
                .build();
        final EventList.Result eventResult = (EventList.Result) esEventList.doExtractResult(searchJob, query, eventList, searchResult,
                metricAggregation, queryContext);
        assertThat(eventResult.events()).containsExactly(
                eventSummaryGenerator("find-1", ImmutableSet.of("stream-id-1")),
                eventSummaryGenerator("find-2", ImmutableSet.of("stream-id-2")),
                eventSummaryGenerator("find-3", ImmutableSet.of("stream-id-1", "stream-id-2"))
        );
    }
}
