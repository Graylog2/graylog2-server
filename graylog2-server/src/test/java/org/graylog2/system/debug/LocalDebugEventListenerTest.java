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
package org.graylog2.system.debug;

import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalDebugEventListenerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Spy
    private EventBus serverEventBus;

    @Before
    public void setUp() {
        DebugEventHolder.setLocalDebugEvent(null);
        new LocalDebugEventListener(serverEventBus);
    }

    @Test
    public void testHandleDebugEvent() throws Exception {
        DebugEvent event = DebugEvent.create("Node ID", "Test");
        assertThat(DebugEventHolder.getLocalDebugEvent()).isNull();
        serverEventBus.post(event);
        assertThat(DebugEventHolder.getLocalDebugEvent()).isSameAs(event);
    }
}