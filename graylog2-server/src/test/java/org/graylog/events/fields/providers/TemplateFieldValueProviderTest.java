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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableMap;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.event.TestEvent;
import org.graylog.events.fields.FieldValue;
import org.graylog.events.fields.FieldValueType;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateFieldValueProviderTest extends FieldValueProviderTest {
    private final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapperProvider().get();
        mapper.registerSubtypes(new NamedType(TemplateFieldValueProvider.Config.class, TemplateFieldValueProvider.Config.TYPE_NAME));
        return mapper;
    }

    private TemplateFieldValueProvider.Config readConfig(String json) throws Exception {
        return (TemplateFieldValueProvider.Config) objectMapper.readValue(json, FieldValueProvider.Config.class);
    }

    private TemplateFieldValueProvider newTemplate(String template, boolean requireValues, boolean includeEmptyFields) {
        return new TemplateFieldValueProvider(TemplateFieldValueProvider.Config.builder()
                .template(template)
                .requireValues(requireValues)
                .includeEmptyFields(includeEmptyFields)
                .build(), Engine.createEngine());
    }

    private TemplateFieldValueProvider newTemplate(String template, boolean requireValues) {
        return newTemplate(template, requireValues, false);
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
    @Disabled("template engine doesn't support expressions")
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

    @Test
    public void includeEmptyFieldsDefaultsToFalse() {
        final TemplateFieldValueProvider.Config config = TemplateFieldValueProvider.Config.builder()
                .template("${source.hello}")
                .build();

        assertThat(config.includeEmptyFields()).isFalse();
    }

    @Test
    public void requireValuesForcesIncludeEmptyFieldsOff() {
        final TemplateFieldValueProvider.Config config = TemplateFieldValueProvider.Config.builder()
                .template("${source.hello}")
                .includeEmptyFields(true)
                .requireValues(true)
                .build();

        assertThat(config.includeEmptyFields()).isFalse();
    }

    @Test
    public void requireValuesForcesIncludeEmptyFieldsOffRegardlessOfSetterOrder() {
        final TemplateFieldValueProvider.Config config = TemplateFieldValueProvider.Config.builder()
                .template("${source.hello}")
                .requireValues(true)
                .includeEmptyFields(true)
                .build();

        assertThat(config.includeEmptyFields()).isFalse();
    }

    @Test
    public void deserializationNormalizesTheInvalidCombination() throws Exception {
        final TemplateFieldValueProvider.Config config = readConfig(
                "{\"type\":\"template-v1\",\"template\":\"${source.hello}\",\"require_values\":true,\"include_empty_fields\":true}");

        assertThat(config.requireValues()).isTrue();
        assertThat(config.includeEmptyFields()).isFalse();
    }

    @Test
    public void deserializationDefaultsIncludeEmptyFieldsToFalseWhenAbsent() throws Exception {
        // This is the on-disk shape of an event definition that has not been migrated yet.
        final TemplateFieldValueProvider.Config config = readConfig(
                "{\"type\":\"template-v1\",\"template\":\"${source.hello}\",\"require_values\":false}");

        assertThat(config.includeEmptyFields()).isFalse();
    }

    @Test
    public void serializationRoundTripPreservesIncludeEmptyFields() throws Exception {
        final TemplateFieldValueProvider.Config config = TemplateFieldValueProvider.Config.builder()
                .template("${source.hello}")
                .includeEmptyFields(true)
                .build();

        final String json = objectMapper.writeValueAsString(config);

        assertThat(json).contains("include_empty_fields");
        assertThat(readConfig(json)).isEqualTo(config);
    }

    @Test
    public void toBuilderRoundTripPreservesIncludeEmptyFields() throws Exception {
        final TemplateFieldValueProvider.Config config = TemplateFieldValueProvider.Config.builder()
                .template("${source.hello}")
                .includeEmptyFields(true)
                .build();

        assertThat(config.toBuilder().build()).isEqualTo(config);
    }

    @Test
    public void emptyTemplateIsExcludedWhenIncludeEmptyFieldsIsOff() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("hello", "world")));

        final FieldValue fieldValue = newTemplate("${source.missing}", false, false).doGet("test", eventWithContext);

        assertThat(fieldValue.isAbsent()).isTrue();
    }

    @Test
    public void emptyTemplateIsIncludedWhenIncludeEmptyFieldsIsOn() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("hello", "world")));

        final FieldValue fieldValue = newTemplate("${source.missing}", false, true).doGet("test", eventWithContext);

        assertThat(fieldValue.isAbsent()).isFalse();
        assertThat(fieldValue.dataType()).isEqualTo(FieldValueType.STRING);
        assertThat(fieldValue.value()).isEmpty();
    }

    @Test
    public void nonEmptyTemplateIsIncludedWhenIncludeEmptyFieldsIsOff() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("hello", "world")));

        final FieldValue fieldValue = newTemplate("${source.hello}", false, false).doGet("test", eventWithContext);

        assertThat(fieldValue.isAbsent()).isFalse();
        assertThat(fieldValue.value()).isEqualTo("world");
    }

    @Test
    public void templateWithLiteralsIsIncludedEvenWhenAllVariablesAreMissing() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("hello", "world")));

        final FieldValue fieldValue = newTemplate("${source.a} - ${source.b}", false, false).doGet("test", eventWithContext);

        assertThat(fieldValue.isAbsent()).isFalse();
        assertThat(fieldValue.value()).isEqualTo(" - ");
    }

    @Test
    public void syntaxErrorStillReturnsErrorWhenIncludeEmptyFieldsIsOff() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("hello", "world")));

        final FieldValue fieldValue = newTemplate("hello: ${source.hello", false, false).doGet("test", eventWithContext);

        assertThat(fieldValue.dataType()).isEqualTo(FieldValueType.ERROR);
        assertThat(fieldValue.isAbsent()).isFalse();
    }
}
