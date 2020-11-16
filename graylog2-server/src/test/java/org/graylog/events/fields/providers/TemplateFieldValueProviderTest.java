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

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableMap;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.event.TestEvent;
import org.graylog.events.fields.FieldValue;
import org.graylog.events.fields.FieldValueType;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateFieldValueProviderTest extends FieldValueProviderTest {
    private TemplateFieldValueProvider newTemplate(String template, boolean requireValues) {
        return new TemplateFieldValueProvider(TemplateFieldValueProvider.Config.builder()
                .template(template)
                .requireValues(requireValues)
                .build(), Engine.createEngine());
    }

    private TemplateFieldValueProvider newTemplate(String template) {
        return newTemplate(template, false);
    }

    @Test
    public void templateWithMessageContext() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("hello", "world")));

        final FieldValue fieldValue = newTemplate("hello: ${source.hello}").doGet("test", eventWithContext);

        assertThat(fieldValue.value()).isEqualTo("hello: world");
    }

    @Test
    public void templateWithEventContext() {
        final TestEvent event = new TestEvent();
        final TestEvent eventContext = new TestEvent();

        eventContext.setField("hello", FieldValue.string("event"));

        final EventWithContext eventWithContext = EventWithContext.create(event, eventContext);

        final FieldValue fieldValue = newTemplate("hello: ${source.hello}").doGet("test", eventWithContext);

        assertThat(fieldValue.value()).isEqualTo("hello: event");
    }

    @Test
    public void templateWithError() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("hello", "world")));

        final FieldValue fieldValue = newTemplate("hello: ${source.yolo}", true).doGet("test", eventWithContext);

        assertThat(fieldValue.dataType()).isEqualTo(FieldValueType.ERROR);
    }

    @Test
    public void templateWithSyntaxError() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("hello", "world")));

        final FieldValue fieldValue = newTemplate("hello: ${source.hello").doGet("test", eventWithContext);

        assertThat(fieldValue.dataType()).isEqualTo(FieldValueType.ERROR);
    }

    @Test
    @Ignore("template engine doesn't support expressions")
    public void templateCalculation() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("bytes", 1024)));

        final FieldValue fieldValue = newTemplate("${source.bytes / 1024}").doGet("test", eventWithContext);

        assertThat(fieldValue.value()).isEqualTo("1");
    }

    @Test
    public void templateNumberFormatting() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("count", 10241234, "avg", 1024.42)));

        final FieldValue fieldValue = newTemplate("count: ${source.count} avg: ${source.avg}").doGet("test", eventWithContext);

        assertThat(fieldValue.value()).isEqualTo("count: 10241234 avg: 1024.42");
    }

    @Test
    public void templateDateFormatting() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("timestamp", DateTime.parse("2019-07-02T12:21:00.123Z"))));

        final FieldValue fieldValue = newTemplate("timestamp: ${source.timestamp}").doGet("test", eventWithContext);

        assertThat(fieldValue.value()).isEqualTo("timestamp: 2019-07-02T12:21:00.123Z");
    }

    @Test
    public void templateBooleanFormatting() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("success", true)));

        final FieldValue fieldValue = newTemplate("success: ${source.success}").doGet("test", eventWithContext);

        assertThat(fieldValue.value()).isEqualTo("success: true");
    }
}