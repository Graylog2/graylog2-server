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
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputStopped;
import org.graylog2.shared.SuppressForbidden;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@SuppressForbidden("Executors#newSingleThreadScheduledExecutor() is okay for tests")
public class StaticFieldFilterTest {
    private final MessageFactory messageFactory = new TestMessageFactory();

    @Mock
    private InputService inputService;
    @Mock
    private Input input;

    private ScheduledExecutorService scheduler;

    @BeforeEach
    void setUp() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    @Test
    public void testFilter() throws Exception {
        Message msg = messageFactory.createMessage("hello", "junit", Tools.nowUTC());
        msg.setSourceInputId("someid");

        when(input.getId()).thenReturn("someid");
        when(inputService.all()).thenReturn(Collections.singletonList(input));
        when(inputService.find(eq("someid"))).thenReturn(input);
        when(inputService.getStaticFields(eq(input.getId())))
                .thenReturn(Collections.singletonList(Maps.immutableEntry("foo", "bar")));

        final StaticFieldFilter filter = new StaticFieldFilter(inputService, new EventBus(), scheduler);
        filter.lifecycleChanged(Lifecycle.STARTING);
        filter.filter(msg);

        assertEquals("hello", msg.getMessage());
        assertEquals("junit", msg.getSource());
        assertEquals("bar", msg.getField("foo"));
    }

    @Test
    public void testFilterIsNotOverwritingExistingKeys() {
        Message msg = messageFactory.createMessage("hello", "junit", Tools.nowUTC());
        msg.addField("foo", "IWILLSURVIVE");

        final StaticFieldFilter filter = new StaticFieldFilter(inputService, new EventBus(), scheduler);
        filter.lifecycleChanged(Lifecycle.STARTING);
        filter.filter(msg);

        assertEquals("hello", msg.getMessage());
        assertEquals("junit", msg.getSource());
        assertEquals("IWILLSURVIVE", msg.getField("foo"));
    }

    @Test
    public void stoppedInputRetainsStaticFieldsForJournalMessages() {
        final String inputId = "someid";

        when(input.getId()).thenReturn(inputId);
        when(inputService.all()).thenReturn(Collections.singletonList(input));
        when(inputService.getStaticFields(eq(inputId)))
                .thenReturn(Collections.singletonList(Maps.immutableEntry("foo", "bar")));

        final EventBus eventBus = new EventBus();
        final StaticFieldFilter filter = new StaticFieldFilter(inputService, eventBus, scheduler);
        filter.lifecycleChanged(Lifecycle.STARTING);

        // Stop the input — static fields should remain cached
        eventBus.post(InputStopped.create(inputId));

        // Simulate a message from the journal arriving after the input was stopped
        final Message msg = messageFactory.createMessage("hello", "junit", Tools.nowUTC());
        msg.setSourceInputId(inputId);
        filter.filter(msg);

        assertEquals("bar", msg.getField("foo"));
    }

    @Test
    public void deletedInputClearsStaticFieldsCache() {
        final String inputId = "someid";

        when(input.getId()).thenReturn(inputId);
        when(inputService.all()).thenReturn(Collections.singletonList(input));
        when(inputService.getStaticFields(eq(inputId)))
                .thenReturn(Collections.singletonList(Maps.immutableEntry("foo", "bar")));

        final EventBus eventBus = new EventBus();
        final StaticFieldFilter filter = new StaticFieldFilter(inputService, eventBus, scheduler);
        filter.lifecycleChanged(Lifecycle.STARTING);

        // Delete the input — static fields should be removed from cache
        eventBus.post(InputDeleted.create(inputId));

        final Message msg = messageFactory.createMessage("hello", "junit", Tools.nowUTC());
        msg.setSourceInputId(inputId);
        filter.filter(msg);

        assertNull(msg.getField("foo"));
    }
}
