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
package org.graylog.events.event;

import com.google.common.collect.ImmutableSet;
import de.huxhorn.sulky.ulid.ULID;
import org.graylog.events.processor.EventDefinition;
import org.graylog.util.Hostname;
import org.graylog.util.HostnameProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventProcessorEventFactoryTest {

    @Test
    void createEventCopiesTagsFromDefinition() {
        final HostnameProvider hostnameProvider = mock(HostnameProvider.class);
        when(hostnameProvider.get()).thenReturn(Hostname.create("test-host", "test-host.example.com"));
        final EventProcessorEventFactory factory = new EventProcessorEventFactory(new ULID(), hostnameProvider);

        final EventDefinition definition = mock(EventDefinition.class);
        when(definition.id()).thenReturn("definition-1");
        when(definition.priority()).thenReturn(2);
        when(definition.alert()).thenReturn(true);
        final org.graylog.events.processor.EventProcessorConfig config = mock(org.graylog.events.processor.EventProcessorConfig.class);
        when(config.type()).thenReturn("aggregation-v1");
        when(definition.config()).thenReturn(config);
        when(definition.tags()).thenReturn(ImmutableSet.of("phishing", "lateral-movement"));

        final Event event = factory.createEvent(definition, DateTime.now(DateTimeZone.UTC), "test message");

        assertThat(event.getTags()).containsExactlyInAnyOrder("phishing", "lateral-movement");
        assertThat(event.toDto().tags()).containsExactlyInAnyOrder("phishing", "lateral-movement");
    }
}
