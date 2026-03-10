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
package org.graylog.events.processor.modifier;

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.event.TestEvent;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.aggregation.AggregationConditions;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class EventSummaryModifierTest {
    private static final int SEARCH_WINDOW_MS = 30000;
    private static final String QUERY_STRING = "aQueryString";
    private final MessageFactory messageFactory = new TestMessageFactory();

    @Test
    void appliesCustomEventSummaryTemplateFromMessageContext() throws Exception {
        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), null, emptyList())
                .toBuilder()
                .eventSummaryTemplate("${source.user_name} failed to log in on ${source.host_name}")
                .build();

        final EventSummaryModifier modifier = new EventSummaryModifier(new Engine());

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final TestEvent event = new TestEvent(now);
        final Message message = messageFactory.createMessage("message", "src", now);
        message.addField("user_name", "test_user");
        message.addField("host_name", "host0001");
        message.addField("message", "message");

        final EventWithContext eventWithContext = EventWithContext.create(event, message);
        modifier.accept(eventWithContext, eventDefinitionDto);

        assertThat(event.getMessage()).isEqualTo("test_user failed to log in on host0001");
    }

    @Test
    void appliesCustomEventSummaryTemplateFromAggregationMessageContext() throws Exception {
        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), null, emptyList())
                .toBuilder()
                .eventSummaryTemplate("Aggregation on ${source.group_field_one} and ${source.group_field_two} has source count of ${source.aggregation_value_count_source}")
                .build();

        final EventSummaryModifier modifier = new EventSummaryModifier(new Engine());

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final TestEvent event = new TestEvent(now);
        final Message message = messageFactory.createMessage("message", "src", now);
        message.addField("group_field_one", "one");
        message.addField("group_field_two", "two");
        message.addField("aggregation_value_count_source", 42.0d);

        final EventWithContext eventWithContext = EventWithContext.create(event, message);
        modifier.accept(eventWithContext, eventDefinitionDto);

        assertThat(event.getMessage()).isEqualTo("Aggregation on one and two has source count of 42.0");
    }

    private EventDefinitionDto buildEventDefinitionDto(
            Set<String> testStreams, List<SeriesSpec> testSeries, AggregationConditions testConditions, List<UsedSearchFilter> filters) {
        return EventDefinitionDto.builder()
                .id("dto-id-1")
                .title("Test Aggregation")
                .description("A test aggregation event processors")
                .priority(1)
                .alert(false)
                .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
                .config(AggregationEventProcessorConfig.builder()
                        .query(QUERY_STRING)
                        .filters(filters)
                        .streams(testStreams)
                        .groupBy(ImmutableList.of("group_field_one", "group_field_two"))
                        .series(testSeries)
                        .conditions(testConditions)
                        .searchWithinMs(SEARCH_WINDOW_MS)
                        .executeEveryMs(SEARCH_WINDOW_MS)
                        .build())
                .keySpec(ImmutableList.of())
                .build();
    }
}
