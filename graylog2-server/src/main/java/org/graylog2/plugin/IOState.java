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

    /**
     * The runtime state of an input on a single node.
     * <p>
     * An {@code IOState} is held per node in the {@code InputRegistry}. Every transition goes through
     * {@link IOState#setState} and is published as an {@link IOStateChangedEvent}; its subscribers
     * ({@code InputStateListener}) raise or clear notifications, write system messages and persist the runtime state,
     * so the state is also visible to other nodes and the UI.
     * <p>
     * Not to be confused with the <em>desired</em> state of an input ({@code MessageInput#getDesiredState()}), which
     * is persisted with the input definition and can only be {@link #RUNNING}, {@link #STOPPED} or {@link #SETUP}. The
     * desired state expresses what the user wants; this type describes what the node actually does.
     * <p>
     * Typical lifecycle:
     * <pre>
     *                  (InputLauncher#launch)
     *   CREATED ---------------------------------> STARTING ---> RUNNING <---> FAILING
     *     |                                           |             |             |
     *     | desired state SETUP                       | launch      |             |
     *     v                                           v error       |             |
     *   SETUP (registered, not launched)            FAILED          |             |
     *                                                               v             v
     *                                            STOPPING ------> STOPPED ------> TERMINATED
     *                                         (InputRegistry#stop)       (InputRegistry#remove)
     * </pre>
     * <ol>
     *     <li>A new {@code IOState} starts as {@link #CREATED} when the input is registered for launching.</li>
     *     <li>If the input's desired state is {@link #SETUP}, it is parked in {@link #SETUP}: registered, but not
     *     launched, so it does not accept messages.</li>
     *     <li>Otherwise the launcher checks the configuration, moves to {@link #STARTING} while
     *     {@code MessageInput#launch} runs and to {@link #RUNNING} once it returned. A launch error ends in
     *     {@link #FAILED}.</li>
     *     <li>While running, the input itself can report recoverable problems via {@code InputFailureRecorder},
     *     toggling between {@link #RUNNING} and {@link #FAILING}.</li>
     *     <li>Stopping moves through {@link #STOPPING} to {@link #STOPPED}; a stopped input stays in the registry so it
     *     remains visible. Removing the input (deletion, update, or switching to setup mode) ends in
     *     {@link #TERMINATED}, after which the {@code IOState} is dropped from the registry and its persisted runtime
     *     state is deleted.</li>
     * </ol>
     * Inputs running on forwarders report their state via gRPC ({@code StateReportHandler}); the forwarder protocol has
     * its own copy of these states, which is why some of the values below are only ever set by forwarders.
     */
    public enum Type {
        /** Initial state of a freshly created {@code IOState}: the input is registered but nothing has happened yet. */
        CREATED,
        /**
         * The input is in setup mode: registered on the node but deliberately not launched, so it receives no
         * messages. Entered when the desired state is {@code SETUP}. Also a valid desired state.
         */
        SETUP,
        /** Only reported by forwarders: the input has been initialized but not yet started. Not set by the server. */
        INITIALIZED,
        /** Only reported by forwarders: the input cannot be started because its configuration is invalid. */
        INVALID_CONFIGURATION,
        /** The configuration check passed and {@code MessageInput#launch} is in progress. */
        STARTING,
        /**
         * The input launched successfully and is accepting messages. Also the state an input returns to from
         * {@link #FAILING} once its problem resolved itself. Also a valid (and the default) desired state.
         */
        RUNNING,
        /**
         * The input failed to launch (configuration check or {@code MessageInput#launch} threw). It is not running;
         * {@code lastFailedAt} is set. Updating the input retries the launch.
         */
        FAILED,
        /** The input is being stopped ({@code MessageInput#stop} is in progress). */
        STOPPING,
        /**
         * The input has been stopped and does not accept messages, but is still registered so it remains visible and
         * can be restarted. Also a valid desired state, for inputs deliberately stopped by the user.
         */
        STOPPED,
        /**
         * Final state: the input has been stopped, terminated and removed from the registry, e.g. because it was
         * deleted or is about to be relaunched with a new configuration. Its persisted runtime state is deleted.
         */
        TERMINATED,
        /**
         * The input launched but is currently experiencing errors at runtime (e.g. it cannot reach a remote source),
         * reported via {@code InputFailureRecorder}. It may recover and return to {@link #RUNNING} on its own, unless
         * the failure was reported as terminal.
         */
        FAILING,
        UNRECOGNIZED // not a real state, but this helps with forwarder compatibility (see StateReportHandler)
    }

    protected T stoppable;
    private final EventBus eventbus;
    protected Type state;
    protected DateTime startedAt;
    protected DateTime lastFailedAt;
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
        this.lastFailedAt = null;
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

    public boolean canBeStarted() {
        return switch (getState()) {
            case RUNNING, STARTING -> false;
            default -> true;
        };
    }

    public void setState(Type state, String detailedMessage) {
        // A changed message has to be published even when the state itself is unchanged: the notification, the system
        // message and the persisted runtime state are all written by IOStateChangedEvent subscribers, so suppressing
        // the event would leave a replacement message visible only to callers reading this object directly.
        final boolean detailedMessageChanged = !Objects.equals(this.detailedMessage, detailedMessage);
        this.setDetailedMessage(detailedMessage);

        if (this.state == state && !detailedMessageChanged) {
            return;
        }
        final IOStateChangedEvent<T> evt = IOStateChangedEvent.create(this.state, state, this);
        this.state = state;
        if (state == Type.FAILED) {
            this.lastFailedAt = Tools.nowUTC();
        }

        this.eventbus.post(evt);
    }

    public void setState(Type state) {
        setState(state, null);
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

    public DateTime getLastFailedAt() {
        return lastFailedAt;
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
                ", lastFailedAt=" + lastFailedAt +
                ", detailedMessage='" + detailedMessage + '\'' +
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
