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
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.configuration.Configuration;

import javax.inject.Inject;
import java.util.Set;

public class AlarmCallbackFactory {
    private Injector injector;
    private final Set<Class<? extends AlarmCallback>> availableAlarmCallbacks;

    @Inject
    public AlarmCallbackFactory(Injector injector,
                                Set<Class<? extends AlarmCallback>> availableAlarmCallbacks) {
        this.injector = injector;
        this.availableAlarmCallbacks = availableAlarmCallbacks;
    }

    public AlarmCallback create(AlarmCallbackConfiguration configuration) throws ClassNotFoundException, AlarmCallbackConfigurationException {
        AlarmCallback alarmCallback = create(configuration.getType());
        alarmCallback.initialize(new Configuration(configuration.getConfiguration()));

        return alarmCallback;
    }

    public AlarmCallback create(String type) throws ClassNotFoundException {
        for (Class<? extends AlarmCallback> availableClass : availableAlarmCallbacks) {
            if (availableClass.getCanonicalName().equals(type))
                return create(availableClass);
        }
        throw new RuntimeException("No class found for type " + type);
    }

    public AlarmCallback create(Class<? extends AlarmCallback> alarmCallbackClass) {
        return injector.getInstance(alarmCallbackClass);
    }
}
