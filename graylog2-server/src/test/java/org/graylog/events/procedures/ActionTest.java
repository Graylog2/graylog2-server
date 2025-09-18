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
    private static final String COMPLEX_SEARCH_QUERY_URI_SAFE = "+(field1:\"hello+world\"~2+OR+field2:[*+TO+2025-12-31])+AND+-(field3:/[a-z]+.*/+OR+field4:(foo*+bar?))+NOT+title:(test^2.0+OR+\"bad:value\")+&&+field5:{1+TO+10}+||+field6:(a+&&+b+||+c)+\\+escaped\\:colon";
    private static final String TIMERANGE_START = "2016-06-08T18:36:20.000Z";
    private static final String TIMERANGE_END = "2025-06-08T18:36:20.000Z";
    private static final String ID = "123abc";

    @Mock
    private EventDto event;

    @Test
    public void testPerformSearchGetLink_query() {
        when(event.replayInfo()).thenReturn(replayInfo());

        String link = performSearchQueryConfig().getLink(event);

        assertThat(link).contains("${http_external_uri}search?q=");
        assertThat(link).contains(COMPLEX_SEARCH_QUERY_URI_SAFE);
        assertThat(link).contains("rangetype=absolute");
        assertThat(link).contains("from=" + TIMERANGE_START);
        assertThat(link).contains("to=" + TIMERANGE_END);
    }

    @Test
    public void testPerformSearchGetLink_savedSearch() {
        when(event.replayInfo()).thenReturn(replayInfo());

        String actionText = performSavedSearchConfig().getLink(event);

        assertThat(actionText).contains("${http_external_uri}views/" + ID);
        assertThat(actionText).contains("rangetype=absolute");
        assertThat(actionText).contains("from=" + TIMERANGE_START);
        assertThat(actionText).contains("to=" + TIMERANGE_END);
        assertThat(actionText).contains("param1=" + "value1");
        assertThat(actionText).contains("param2=" + "value2");
    }

    @Test
    public void testGoToDashboardGetLink() {
        String actionText = goToDashboardConfig().getLink(event);

        assertThat(actionText).contains("${http_external_uri}dashboards/" + ID);
        assertThat(actionText).contains("param1=" + "value1");
        assertThat(actionText).contains("param2=" + "value2");
    }

    @Test
    public void testExecuteNotificationGetLink() {
        when(event.id()).thenReturn(ID);

        String actionText = executeNotificationConfig().getLink(event);

        assertThat(actionText).contains("${http_external_uri}security/security-events/alerts?query=id:${event.id}");
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
