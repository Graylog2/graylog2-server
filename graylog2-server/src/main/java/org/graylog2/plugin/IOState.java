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
package org.graylog2.plugin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.events.inputs.IOStateChangedEvent;
import org.joda.time.DateTime;

import java.util.Objects;

@JsonAutoDetect
public class IOState<T extends Stoppable> {
    public interface Factory<T extends Stoppable> {
        IOState<T> create(T stoppable);
        IOState<T> create(T stoppable, Type state);
    }
    public enum Type {
        CREATED,
        INITIALIZED,
        INVALID_CONFIGURATION,
        STARTING,
        RUNNING,
        FAILED,
        STOPPING,
        STOPPED,
        TERMINATED
    }

    protected T stoppable;
    private EventBus eventbus;
    protected Type state;
    protected DateTime startedAt;
    protected String detailedMessage;

    @AssistedInject
    public IOState(EventBus eventbus, @Assisted T stoppable) {
        this(eventbus, stoppable, Type.CREATED);
    }

    @AssistedInject
    public IOState(EventBus eventbus, @Assisted T stoppable, @Assisted Type state) {
        this.eventbus = eventbus;
        this.state = state;
        this.stoppable = stoppable;
        this.startedAt = Tools.nowUTC();
    }

    public T getStoppable() {
        return stoppable;
    }

    public void setStoppable(T stoppable) {
        this.stoppable = stoppable;
    }

    public Type getState() {
        return state;
    }

    public void setState(Type state) {
        final IOStateChangedEvent<T> evt = IOStateChangedEvent.create(this.state, state, this);

        this.state = state;
        this.setDetailedMessage(null);
        this.eventbus.post(evt);
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(DateTime startedAt) {
        this.startedAt = startedAt;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

    @Override
    public String toString() {
        return "InputState{" +
                "stoppable=" + stoppable +
                ", state=" + state +
                ", startedAt=" + startedAt +
                ", detailedMessage='" + detailedMessage + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IOState that = (IOState) o;

        return Objects.equals(this.stoppable, that.stoppable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stoppable);
    }
}
