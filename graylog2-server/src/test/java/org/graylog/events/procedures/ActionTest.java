/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events.procedures;

import org.graylog.events.event.EventDto;
import org.graylog.events.event.EventReplayInfo;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActionTest {
    private static final String COMPLEX_SEARCH_QUERY = "+(field1:\"hello world\"~2 OR field2:[* TO 2025-12-31]) AND -(field3:/[a-z]+.*/ OR field4:(foo* bar?)) NOT title:(test^2.0 OR \"bad:value\") && field5:{1 TO 10} || field6:(a && b || c) \\+escaped\\:colon";
    private static final String COMPLEX_SEARCH_QUERY_URI_SAFE = "/search?q=%2B%28field1%3A%22hello+world%22%7E2+OR+field2%3A%5B*+TO+2025-12-31%5D%29+AND+-%28field3%3A%2F%5Ba-z%5D%2B.*%2F+OR+field4%3A%28foo*+bar%3F%29%29+NOT+title%3A%28test%5E2.0+OR+%22bad%3Avalue%22%29+%26%26+field5%3A%7B1+TO+10%7D+%7C%7C+field6%3A%28a+%26%26+b+%7C%7C+c%29+%5C%2Bescaped%5C%3Acolon&rangetype=absolute&from=2016-06-08T18%3A36%3A20.000Z&to=2025-06-08T18%3A36%3A20.000Z";
    private static final String TIMERANGE_START = "2016-06-08T18:36:20.000Z";
    private static final String TIMERANGE_END = "2025-06-08T18:36:20.000Z";
    private static final String ID = "123abc";

    @Mock
    private EventDto event;

    @Test
    public void testPerformSearchGetLink_query() {
        when(event.replayInfo()).thenReturn(replayInfo());

        String link = performSearchQueryConfig().getLink(event).toString();

        assertThat(link).contains("search?q=");
        assertThat(link).contains(COMPLEX_SEARCH_QUERY_URI_SAFE);
        assertThat(link).contains("rangetype=absolute");
        assertThat(link).contains("from=2016-06-08T18%3A36%3A20.000Z");
        assertThat(link).contains("to=2025-06-08T18%3A36%3A20.000Z");
    }

    @Test
    public void testPerformSearchGetLink_savedSearch() {
        when(event.replayInfo()).thenReturn(replayInfo());

        String actionText = performSavedSearchConfig().getLink(event).toString();

        assertThat(actionText).contains("views/" + ID);
        assertThat(actionText).contains("rangetype=absolute");
        assertThat(actionText).contains("from=2016-06-08T18%3A36%3A20.000Z");
        assertThat(actionText).contains("to=2025-06-08T18%3A36%3A20.000Z");
        assertThat(actionText).contains("param1=" + "value1");
        assertThat(actionText).contains("param2=" + "value2");
    }

    @Test
    public void testGoToDashboardGetLink() {
        String actionText = goToDashboardConfig().getLink(event).toString();

        assertThat(actionText).contains("dashboards/" + ID);
        assertThat(actionText).contains("param1=" + "value1");
        assertThat(actionText).contains("param2=" + "value2");
    }

    @Test
    public void testExecuteNotificationGetLink() {
        when(event.id()).thenReturn(ID);

        String actionText = executeNotificationConfig().getLink(event).toString();

        assertThat(actionText).contains("security/security-events/alerts?query=id%3A%24%7Bevent.id%7D");
    }

    private ExecuteNotification.Config executeNotificationConfig() {
        return ExecuteNotification.Config.builder()
                .type(ExecuteNotification.NAME)
                .notificationId(ID)
                .build();
    }

    private GoToDashboard.Config goToDashboardConfig() {
        return GoToDashboard.Config.builder()
                .type(GoToDashboard.NAME)
                .dashboardId(ID)
                .parameters(Map.of("param1", "value1", "param2", "value2"))
                .build();
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
                .parameters(Map.of("param1", "value1", "param2", "value2"))
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
