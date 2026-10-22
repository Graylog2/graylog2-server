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
package org.graylog.events.fields;

import org.graylog.events.event.EventWithContext;
import org.graylog.events.event.TestEvent;
import org.graylog.events.fields.providers.FieldValueProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class EventFieldSpecEngineTest {
    private static final String PROVIDER_TYPE = "test-provider";

    // The fake config carries a per-event function so a single provider can be absent for one event
    // in the batch and present for another.
    private record FakeConfig(Function<EventWithContext, FieldValue> valueFn) implements FieldValueProvider.Config {
        @Override
        public String type() {
            return PROVIDER_TYPE;
        }
    }

    private EventFieldSpecEngine newEngine() {
        final FieldValueProvider.Factory factory =
                config -> (fieldName, eventWithContext) -> ((FakeConfig) config).valueFn().apply(eventWithContext);
        return new EventFieldSpecEngine(Map.of(PROVIDER_TYPE, factory));
    }

    private EventFieldSpec specFor(FieldValue... values) {
        return specForFunctions(Arrays.stream(values)
                .<Function<EventWithContext, FieldValue>>map(value -> eventWithContext -> value)
                .toList());
    }

    private EventFieldSpec specForFunctions(List<Function<EventWithContext, FieldValue>> valueFns) {
        return EventFieldSpec.builder()
                .dataType(FieldValueType.STRING)
                .providers(valueFns.stream().<FieldValueProvider.Config>map(FakeConfig::new).toList())
                .build();
    }

    @Test
    void absentValueIsNotSetOnTheEvent() {
        final TestEvent event = new TestEvent();

        newEngine().execute(List.of(EventWithContext.create(event)),
                Map.of("my_field", specFor(FieldValue.absent())));

        assertThat(event.hasField("my_field")).isFalse();
    }

    @Test
    void stringValueIsSetOnTheEvent() {
        final TestEvent event = new TestEvent();

        newEngine().execute(List.of(EventWithContext.create(event)),
                Map.of("my_field", specFor(FieldValue.string("hello"))));

        assertThat(event.getField("my_field").value()).isEqualTo("hello");
    }

    @Test
    void errorValueIsStillSetOnTheEvent() {
        final TestEvent event = new TestEvent();

        newEngine().execute(List.of(EventWithContext.create(event)),
                Map.of("my_field", specFor(FieldValue.error())));

        assertThat(event.getField("my_field").dataType()).isEqualTo(FieldValueType.ERROR);
    }

    @Test
    void absentValueFromLaterProviderLeavesEarlierValueInPlace() {
        final TestEvent event = new TestEvent();

        newEngine().execute(List.of(EventWithContext.create(event)),
                Map.of("my_field", specFor(FieldValue.string("first"), FieldValue.absent())));

        assertThat(event.getField("my_field").value()).isEqualTo("first");
    }

    @Test
    void laterProviderStillAppliesAfterAnAbsentOne() {
        final TestEvent event = new TestEvent();

        newEngine().execute(List.of(EventWithContext.create(event)),
                Map.of("my_field", specFor(FieldValue.absent(), FieldValue.string("second"))));

        assertThat(event.getField("my_field").value()).isEqualTo("second");
    }

    @Test
    void absentValueOnlySkipsTheEventItAppliesTo() {
        final TestEvent withValue = new TestEvent();
        final TestEvent withoutValue = new TestEvent();

        newEngine().execute(
                List.of(EventWithContext.create(withValue), EventWithContext.create(withoutValue)),
                Map.of("my_field", specForFunctions(List.of(
                        eventWithContext -> eventWithContext.event() == withValue
                                ? FieldValue.string("kept")
                                : FieldValue.absent()))));

        assertThat(withValue.getField("my_field").value()).isEqualTo("kept");
        assertThat(withoutValue.hasField("my_field")).isFalse();
    }
}
