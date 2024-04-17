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
package org.graylog.datanode.state;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import org.graylog.datanode.management.StateMachineTracerAggregator;

public class DatanodeStateMachine extends StateMachine<DatanodeState, DatanodeEvent> {
    /**
     * How many times can the OS rest api call fail before we switch to the failed state
     */
    public static final int MAX_REST_TEMPORARY_FAILURES = 3;
    public static final int MAX_REST_STARTUP_FAILURES = 5;
    public static final int MAX_REBOOT_FAILURES = 3;

    StateMachineTracerAggregator tracerAggregator = new StateMachineTracerAggregator();

    public DatanodeStateMachine(DatanodeState initialState, StateMachineConfig<DatanodeState, DatanodeEvent> config) {
        super(initialState, config);
        setTrace(tracerAggregator);
    }

    public static DatanodeStateMachine createNew() {

        final FailuresCounter restFailureCounter = FailuresCounter.oneBased(MAX_REST_TEMPORARY_FAILURES);
        final FailuresCounter startupFailuresCounter = FailuresCounter.oneBased(MAX_REST_STARTUP_FAILURES);
        final FailuresCounter rebootCounter = FailuresCounter.oneBased(MAX_REBOOT_FAILURES);

        StateMachineConfig<DatanodeState, DatanodeEvent> config = new StateMachineConfig<>();

        // Freshly created process, it hasn't started yet and doesn't have any pid.
        config.configure(DatanodeState.WAITING_FOR_CONFIGURATION)
                .permit(DatanodeEvent.PROCESS_PREPARED, DatanodeState.PREPARED)
                // jump to started only allowed to facilitate startup with insecure config
                .permit(DatanodeEvent.PROCESS_STARTED, DatanodeState.STARTING)
                .ignore(DatanodeEvent.PROCESS_STOPPED)
                .ignore(DatanodeEvent.HEALTH_CHECK_FAILED);

        config.configure(DatanodeState.PREPARED)
                .permit(DatanodeEvent.PROCESS_STARTED, DatanodeState.STARTING)
                .permit(DatanodeEvent.PROCESS_TERMINATED, DatanodeState.TERMINATED)
                .permit(DatanodeEvent.PROCESS_STOPPED, DatanodeState.TERMINATED)
                .ignore(DatanodeEvent.HEALTH_CHECK_FAILED);

        // the process has started already, now we have to wait for a running OS and available REST api
        // the startupFailuresCounter keeps track of failed REST status calls and allow failures during the
        // startup period
        config.configure(DatanodeState.STARTING)
                .permitDynamic(DatanodeEvent.HEALTH_CHECK_FAILED,
                        () -> startupFailuresCounter.failedTooManyTimes() ? DatanodeState.FAILED : DatanodeState.STARTING,
                        startupFailuresCounter::increment)
                .permit(DatanodeEvent.HEALTH_CHECK_OK, DatanodeState.AVAILABLE)
                .permit(DatanodeEvent.PROCESS_STOPPED, DatanodeState.TERMINATED)
                .permit(DatanodeEvent.PROCESS_TERMINATED, DatanodeState.TERMINATED);

        // the process is running and responding to the REST status, it's available for any usage
        config.configure(DatanodeState.AVAILABLE)
                .onEntry(restFailureCounter::resetFailuresCounter)
                .onEntry(rebootCounter::resetFailuresCounter)
                .permitReentry(DatanodeEvent.HEALTH_CHECK_OK)
                .permit(DatanodeEvent.HEALTH_CHECK_FAILED, DatanodeState.NOT_RESPONDING)
                .permit(DatanodeEvent.PROCESS_STOPPED, DatanodeState.TERMINATED)
                .permit(DatanodeEvent.PROCESS_TERMINATED, DatanodeState.TERMINATED)
                .permit(DatanodeEvent.PROCESS_REMOVE, DatanodeState.REMOVING)
                .ignore(DatanodeEvent.PROCESS_STARTED);

        // if the REST api is not responding, we'll jump to this state and count how many times the failure
        // occurs. If it fails ttoo many times, we'll mark the process as FAILED
        config.configure(DatanodeState.NOT_RESPONDING)
                .permitDynamic(DatanodeEvent.HEALTH_CHECK_FAILED,
                        () -> restFailureCounter.failedTooManyTimes() ? DatanodeState.FAILED : DatanodeState.NOT_RESPONDING,
                        restFailureCounter::increment
                )
                .permit(DatanodeEvent.HEALTH_CHECK_OK, DatanodeState.AVAILABLE)
                .permit(DatanodeEvent.PROCESS_STOPPED, DatanodeState.TERMINATED)
                .permit(DatanodeEvent.PROCESS_TERMINATED, DatanodeState.TERMINATED);

        // failed and we see the process as not recoverable.
        // TODO: what to do if the process fails? Reboot?
        config.configure(DatanodeState.FAILED)
                .ignore(DatanodeEvent.HEALTH_CHECK_FAILED)
                .permit(DatanodeEvent.HEALTH_CHECK_OK, DatanodeState.AVAILABLE)
                .permit(DatanodeEvent.PROCESS_STOPPED, DatanodeState.TERMINATED)
                .permit(DatanodeEvent.PROCESS_TERMINATED, DatanodeState.TERMINATED);

        // final state, the process is not alive anymore, terminated on the operating system level
        config.configure(DatanodeState.TERMINATED)
                .permit(DatanodeEvent.PROCESS_STARTED, DatanodeState.STARTING, rebootCounter::increment)
                .ignore(DatanodeEvent.HEALTH_CHECK_FAILED)
                .ignore(DatanodeEvent.PROCESS_STOPPED)
                .ignore(DatanodeEvent.PROCESS_TERMINATED); // final state, all following terminate events are ignored

        config.configure(DatanodeState.REMOVING)
                .ignore(DatanodeEvent.HEALTH_CHECK_OK)
                .permit(DatanodeEvent.HEALTH_CHECK_FAILED, DatanodeState.FAILED)
                .permit(DatanodeEvent.PROCESS_STOPPED, DatanodeState.REMOVED);

        config.configure(DatanodeState.REMOVED)
                .permit(DatanodeEvent.RESET, DatanodeState.WAITING_FOR_CONFIGURATION)
                .ignore(DatanodeEvent.PROCESS_STOPPED);

        return new DatanodeStateMachine(DatanodeState.WAITING_FOR_CONFIGURATION, config);
    }

    public StateMachineTracerAggregator getTracerAggregator() {
        return tracerAggregator;
    }
}
