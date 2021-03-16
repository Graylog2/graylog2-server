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
package org.graylog.events.fields.providers;

import org.graylog.events.event.EventWithContext;
import org.graylog.events.event.TestEvent;
import org.graylog.events.fields.FieldValue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FixedValueFieldValueProviderTest extends FieldValueProviderTest {
    private FixedValueFieldValueProvider newFixedValue(String value) {
        return new FixedValueFieldValueProvider(FixedValueFieldValueProvider.Config.builder()
                .value(value)
                .build());
    }

    @Test
    public void fixedValueTest() {
        final TestEvent event = new TestEvent();
        final TestEvent eventContext = new TestEvent();

        eventContext.setField("test", FieldValue.string("hello"));

        final EventWithContext eventWithContext = EventWithContext.create(event, eventContext);

        final FieldValue fieldValue = newFixedValue("hello").doGet("test", eventWithContext);

        assertThat(fieldValue.value()).isEqualTo("hello");
    }

}
