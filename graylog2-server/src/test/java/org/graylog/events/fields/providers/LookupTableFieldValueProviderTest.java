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

import com.google.common.collect.ImmutableMap;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.event.TestEvent;
import org.graylog.events.fields.FieldValue;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class LookupTableFieldValueProviderTest extends FieldValueProviderTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private LookupTableService lookupTableService;
    @Mock
    private LookupTableService.Builder lookupTableServiceBuilder;
    @Mock
    private LookupTableService.Function lookupTableFunction;

    @Before
    public void setUp() throws Exception {

    }

    private void setupMocks(String tableName) {
        when(lookupTableService.hasTable(tableName)).thenReturn(true);
        when(lookupTableService.newBuilder()).thenReturn(lookupTableServiceBuilder);
        when(lookupTableServiceBuilder.lookupTable(tableName)).thenReturn(lookupTableServiceBuilder);
        when(lookupTableServiceBuilder.build()).thenReturn(lookupTableFunction);
    }

    private LookupTableFieldValueProvider.Config newConfig(String tableName, String keyField) {
        return LookupTableFieldValueProvider.Config.builder()
                .tableName(tableName)
                .keyField(keyField)
                .build();
    }

    private LookupTableFieldValueProvider newProvider(LookupTableFieldValueProvider.Config config) {
        return new LookupTableFieldValueProvider(config, lookupTableService);
    }

    @Test
    public void testWithMessageContext() {
        final String fieldValueString = "world";
        final String expectedLookupValue = "lookup-world";

        final TestEvent event = new TestEvent();
        final Message message = newMessage(ImmutableMap.of("hello", fieldValueString));
        final EventWithContext eventWithContext = EventWithContext.create(event, message);

        final LookupTableFieldValueProvider.Config config = newConfig("test", "hello");

        setupMocks("test");
        when(lookupTableFunction.lookup("world")).thenReturn(LookupResult.single("lookup-" + message.getField("hello")));

        final FieldValue fieldValue = newProvider(config).doGet("test", eventWithContext);

        assertThat(fieldValue.value()).isEqualTo(expectedLookupValue);
    }

    @Test
    public void testWithEventContext() {
        final String fieldValueString = "event";
        final String expectedLookupValue = "lookup-event";

        final TestEvent event = new TestEvent();
        final TestEvent eventContext = new TestEvent();

        eventContext.setField("hello", FieldValue.string(fieldValueString));

        final EventWithContext eventWithContext = EventWithContext.create(event, eventContext);

        final LookupTableFieldValueProvider.Config config = newConfig("test", "hello");

        setupMocks("test");
        when(lookupTableFunction.lookup(fieldValueString)).thenReturn(LookupResult.single("lookup-" + eventContext.getField("hello").value()));

        final FieldValue fieldValue = newProvider(config).doGet("test", eventWithContext);

        assertThat(fieldValue.value()).isEqualTo(expectedLookupValue);
    }

    @Test
    public void testWithMissingLookupTable() {
        final TestEvent event = new TestEvent();
        final EventWithContext eventWithContext = EventWithContext.create(event, newMessage(ImmutableMap.of("hello", "world")));

        final LookupTableFieldValueProvider.Config config = newConfig("test-doesntexist", "hello");

        setupMocks("test");
        when(lookupTableFunction.lookup("world")).thenReturn(LookupResult.single("lookup-world"));

        assertThatThrownBy(() -> newProvider(config).doGet("test", eventWithContext))
                .hasMessageContaining("test-doesntexist")
                .isInstanceOf(IllegalArgumentException.class);
    }
}