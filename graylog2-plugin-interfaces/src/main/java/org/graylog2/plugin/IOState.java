/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.joda.time.DateTime;

import java.util.UUID;

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
        this.startedAt = Tools.iso8601();
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
        this.state = state;
        this.setDetailedMessage(null);
        this.eventbus.post(this);
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

        if (!stoppable.equals(that.stoppable)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = stoppable.hashCode();
        result = 31 * result + stoppable.hashCode();
        return result;
    }
}
