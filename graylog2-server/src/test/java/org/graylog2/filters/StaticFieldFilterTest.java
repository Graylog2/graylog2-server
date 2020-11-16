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
package org.graylog2.filters;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.SuppressForbidden;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class StaticFieldFilterTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private InputService inputService;
    @Mock
    private Input input;

    @Test
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    public void testFilter() throws Exception {
        Message msg = new Message("hello", "junit", Tools.nowUTC());
        msg.setSourceInputId("someid");

        when(input.getId()).thenReturn("someid");
        when(inputService.all()).thenReturn(Collections.singletonList(input));
        when(inputService.find(eq("someid"))).thenReturn(input);
        when(inputService.getStaticFields(eq(input)))
                .thenReturn(Collections.singletonList(Maps.immutableEntry("foo", "bar")));

        final StaticFieldFilter filter = new StaticFieldFilter(inputService, new EventBus(), Executors.newSingleThreadScheduledExecutor());
        filter.filter(msg);

        assertEquals("hello", msg.getMessage());
        assertEquals("junit", msg.getSource());
        assertEquals("bar", msg.getField("foo"));
    }

    @Test
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    public void testFilterIsNotOverwritingExistingKeys() throws Exception {
        Message msg = new Message("hello", "junit", Tools.nowUTC());
        msg.addField("foo", "IWILLSURVIVE");

        final StaticFieldFilter filter = new StaticFieldFilter(inputService, new EventBus(), Executors.newSingleThreadScheduledExecutor());
        filter.filter(msg);

        assertEquals("hello", msg.getMessage());
        assertEquals("junit", msg.getSource());
        assertEquals("IWILLSURVIVE", msg.getField("foo"));
    }
}
