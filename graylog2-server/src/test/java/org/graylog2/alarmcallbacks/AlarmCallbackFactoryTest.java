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
package org.graylog2.alarmcallbacks;

import com.google.inject.Injector;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class AlarmCallbackFactoryTest {

    private AlarmCallbackFactory alarmCallbackFactory;
    @Mock
    private Injector injector;
    @Mock
    private DummyAlarmCallback dummyAlarmCallback;

    public interface DummyAlarmCallback extends AlarmCallback {
    }

    @BeforeEach
    public void setUp() throws Exception {
        when(injector.getInstance(DummyAlarmCallback.class)).thenReturn(dummyAlarmCallback);
        Set<Class<? extends AlarmCallback>> availableAlarmCallbacks = new HashSet<Class<? extends AlarmCallback>>();
        availableAlarmCallbacks.add(DummyAlarmCallback.class);

        this.alarmCallbackFactory = new AlarmCallbackFactory(injector, availableAlarmCallbacks);
    }

    @Test
    public void testCreateByAlarmCallbackConfiguration() throws Exception {
        AlarmCallbackConfiguration configuration = mock(AlarmCallbackConfiguration.class);
        when(configuration.getType()).thenReturn(DummyAlarmCallback.class.getCanonicalName());

        AlarmCallback alarmCallback = alarmCallbackFactory.create(configuration);

        assertNotNull(alarmCallback);
        assertTrue(alarmCallback instanceof DummyAlarmCallback);
        assertEquals(dummyAlarmCallback, alarmCallback);
    }

    @Test
    public void testCreateByClassName() throws Exception {
        String className = DummyAlarmCallback.class.getCanonicalName();

        AlarmCallback alarmCallback = alarmCallbackFactory.create(className);

        assertNotNull(alarmCallback);
        assertTrue(alarmCallback instanceof DummyAlarmCallback);
        assertEquals(dummyAlarmCallback, alarmCallback);
    }

    @Test
    public void testCreateByClass() throws Exception {
        AlarmCallback alarmCallback = alarmCallbackFactory.create(DummyAlarmCallback.class);

        assertTrue(alarmCallback instanceof DummyAlarmCallback);
        assertEquals(dummyAlarmCallback, alarmCallback);
    }
}
