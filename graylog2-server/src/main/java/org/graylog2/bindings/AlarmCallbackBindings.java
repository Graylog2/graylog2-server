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
package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alarmcallbacks.HTTPAlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlarmCallbackBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<AlarmCallback> alarmCallbackBinder = Multibinder.newSetBinder(binder(), AlarmCallback.class);
        alarmCallbackBinder.addBinding().to(EmailAlarmCallback.class);
        alarmCallbackBinder.addBinding().to(HTTPAlarmCallback.class);

        TypeLiteral<Class<? extends AlarmCallback>> type = new TypeLiteral<Class<? extends AlarmCallback>>(){};
        Multibinder<Class<? extends AlarmCallback>> alarmCallbackClassBinder = Multibinder.newSetBinder(binder(), type);
        alarmCallbackClassBinder.addBinding().toInstance(EmailAlarmCallback.class);
        alarmCallbackClassBinder.addBinding().toInstance(HTTPAlarmCallback.class);
    }
}
