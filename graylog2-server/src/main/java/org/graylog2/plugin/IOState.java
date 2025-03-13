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
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.inputs.MessageInputFailure;
import org.graylog.inputs.state.MessageInputStateMachine;
import org.graylog2.plugin.inputs.MessageInput;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@JsonAutoDetect
public class IOState<T extends Stoppable> {
    private static final Logger LOG = LoggerFactory.getLogger(IOState.class);

    public interface Factory<T extends Stoppable> {
        IOState<T> create(T stoppable);
        IOState<T> create(T stoppable, Type state);
    }

    public enum Type {
        CREATED,
        SETUP,
        INITIALIZED,
        INVALID_CONFIGURATION,
        STARTING,
        RUNNING,
        FAILED,
        STOPPING,
        STOPPED,
        TERMINATED,
        FAILING,
        UNRECOGNIZED // not a real state, but this helps with forwarder compatibility (see StateReportHandler)
    }

    public enum Trigger {
        START, RUNNING, FAIL, STOP, STOPPED, TERMINATE, SETUP
    }

    protected T stoppable;
    protected DateTime startedAt;
    protected String detailedMessage;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Duration lockTimeout = Duration.ofMillis(100); // TODO: Make lock timeout configurable?
    private final MessageInputStateMachine stateMachine;

    @AssistedInject
    public IOState(MessageInputStateMachine.Factory stateMachineFactory, @Assisted T stoppable) {
        this(stateMachineFactory, stoppable, Type.CREATED);
    }

    @AssistedInject
    public IOState(MessageInputStateMachine.Factory stateMachineFactory, @Assisted T stoppable, @Assisted Type state) {
        this.stoppable = stoppable;
        this.startedAt = Tools.nowUTC();
        //noinspection unchecked
        this.stateMachine = stateMachineFactory.create(state, (IOState<MessageInput>) this, (MessageInput) stoppable);
    }

    public T getStoppable() {
        return stoppable;
    }

    public void setStoppable(T stoppable) {
        this.stoppable = stoppable;
    }

    public Type getState() {
        return stateMachine.getState();
    }

    public boolean canBeStarted() {
        return switch (getState()) {
            case RUNNING, STARTING -> false;
            default -> true;
        };
    }

    public void triggerSetup() {
        trigger(Trigger.SETUP);
    }

    public void triggerStart() {
        trigger(Trigger.START);
    }

    public void triggerRunning() {
        trigger(Trigger.RUNNING);
    }

    public void triggerStop() {
        trigger(Trigger.STOP);
    }

    public void triggerStopped() {
        trigger(Trigger.STOPPED);
    }

    public void triggerTerminate() {
        trigger(Trigger.TERMINATE);
    }

    public void triggerFail(MessageInputFailure failure) {
        withLock(stateMachine.failTrigger().getTrigger(), () -> stateMachine.fire(stateMachine.failTrigger(), failure));
    }

    private void trigger(Trigger trigger) {
        withLock(trigger, () -> stateMachine.fire(trigger));
    }

    private void withLock(Trigger trigger, Runnable runnable) {
        try {
            if (!lock.tryLock(lockTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                LOG.warn("Couldn't acquire lock to fire trigger: {} (timeout: {})", trigger, lockTimeout);
                return;
            }
            runnable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
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
        return "IOState{" +
                "stoppable=" + stoppable +
                ", startedAt=" + startedAt +
                ", detailedMessage='" + detailedMessage + '\'' +
                ", stateMachine=" + stateMachine +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IOState that = (IOState) o;

        return Objects.equals(this.stoppable, that.stoppable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stoppable);
    }
}
