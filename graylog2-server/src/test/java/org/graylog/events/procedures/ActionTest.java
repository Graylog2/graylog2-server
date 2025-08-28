package org.graylog.events.procedures;

import org.graylog.events.event.EventDto;
import org.graylog.events.event.EventReplayInfo;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActionTest {
    private static final String COMPLEX_SEARCH_QUERY = "+(field1:\"hello world\"~2 OR field2:[* TO 2025-12-31]) AND -(field3:/[a-z]+.*/ OR field4:(foo* bar?)) NOT title:(test^2.0 OR \"bad:value\") && field5:{1 TO 10} || field6:(a && b || c) \\+escaped\\:colon";
    private static final String COMPLEX_SEARCH_QUERY_URI_SAFE = "+(field1:\"hello+world\"~2+OR+field2:[*+TO+2025-12-31])+AND+-(field3:/[a-z]+.*/+OR+field4:(foo*+bar?))+NOT+title:(test^2.0+OR+\"bad:value\")+&&+field5:{1+TO+10}+||+field6:(a+&&+b+||+c)+\\+escaped\\:colon";
    private static final String TIMERANGE_START = "2016-06-08T18:36:20.000Z";
    private static final String TIMERANGE_END = "2025-06-08T18:36:20.000Z";
    private static final String ID = "123abc";

    @Mock
    private EventDto event;

    @Test
    public void testPerformSearchToText_query() {
        when(event.replayInfo()).thenReturn(replayInfo());

        String actionText = performSearchQueryConfig().toText(event);

        assertThat(actionText).contains("${http_external_uri}search?q=");
        assertThat(actionText).contains(COMPLEX_SEARCH_QUERY_URI_SAFE);
        assertThat(actionText).contains("rangetype=absolute");
        assertThat(actionText).contains("from=" + TIMERANGE_START);
        assertThat(actionText).contains("to=" + TIMERANGE_END);
    }

    @Test
    public void testPerformSearchToText_savedSearch() {
        when(event.replayInfo()).thenReturn(replayInfo());

        String actionText = performSavedSearchConfig().toText(event);

        assertThat(actionText).contains("${http_external_uri}views/" + ID);
        assertThat(actionText).contains("rangetype=absolute");
        assertThat(actionText).contains("from=" + TIMERANGE_START);
        assertThat(actionText).contains("to=" + TIMERANGE_END);
    }

    private PerformSearch.Config performSearchQueryConfig() {
        return PerformSearch.Config.builder()
                .type(PerformSearch.NAME)
                .useSavedSearch(false)
                .query(COMPLEX_SEARCH_QUERY)
                .build();
    }

    private PerformSearch.Config performSavedSearchConfig() {
        return PerformSearch.Config.builder()
                .type(PerformSearch.NAME)
                .useSavedSearch(true)
                .savedSearch(ID)
                .build();
    }

    private Optional<EventReplayInfo> replayInfo() {
        return Optional.of(EventReplayInfo.builder()
                .timerangeStart(DateTime.parse(TIMERANGE_START))
                .timerangeEnd(DateTime.parse(TIMERANGE_END))
                .query("")
                .streams(Set.of())
                .build());
    }
}
