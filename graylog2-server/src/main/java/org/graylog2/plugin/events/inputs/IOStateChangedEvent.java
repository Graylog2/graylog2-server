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
package org.graylog2.plugin.events.inputs;

import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.Stoppable;

@AutoValue
@WithBeanGetter
public abstract class IOStateChangedEvent<T extends Stoppable> {
    public abstract IOState.Type oldState();
    public abstract IOState.Type newState();
    public abstract IOState<T> changedState();

    public static <K extends Stoppable> IOStateChangedEvent<K> create(IOState.Type oldState, IOState.Type newState, IOState<K> changedEvent) {
        return new AutoValue_IOStateChangedEvent<>(oldState, newState, changedEvent);
    }
}
