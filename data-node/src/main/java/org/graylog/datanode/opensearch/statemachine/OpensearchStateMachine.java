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
package org.graylog.datanode.opensearch.statemachine;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.tracer.StateMachineTracerAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpensearchStateMachine extends StateMachine<OpensearchState, OpensearchEvent> {

    private final Logger log = LoggerFactory.getLogger(OpensearchStateMachine.class);

    /**
     * How many times can the OS rest api call fail before we switch to the failed state
     */
    public static final int MAX_REST_TEMPORARY_FAILURES = 3;
    public static final int MAX_REST_STARTUP_FAILURES = 5;
    public static final int MAX_REBOOT_FAILURES = 3;

    StateMachineTracerAggregator tracerAggregator = new StateMachineTracerAggregator();
    private static OpensearchProcess process;

    public OpensearchStateMachine(OpensearchState initialState, StateMachineConfig<OpensearchState, OpensearchEvent> config) {
        super(initialState, config);
        setTrace(tracerAggregator);
    }

    public static OpensearchStateMachine createNew(OpensearchProcess process) {
        OpensearchStateMachine.process = process;
        final FailuresCounter restFailureCounter = FailuresCounter.oneBased(MAX_REST_TEMPORARY_FAILURES);
        final FailuresCounter startupFailuresCounter = FailuresCounter.oneBased(MAX_REST_STARTUP_FAILURES);
        final FailuresCounter rebootCounter = FailuresCounter.oneBased(MAX_REBOOT_FAILURES);

        StateMachineConfig<OpensearchState, OpensearchEvent> config = new StateMachineConfig<>();

        // Freshly created process, it hasn't started yet and doesn't have any pid.
        config.configure(OpensearchState.WAITING_FOR_CONFIGURATION)
                .permit(OpensearchEvent.PROCESS_PREPARED, OpensearchState.PREPARED)
                // jump to started only allowed to facilitate startup with insecure config
                .permit(OpensearchEvent.PROCESS_STARTED, OpensearchState.STARTING)
                .ignore(OpensearchEvent.PROCESS_STOPPED)
                .ignore(OpensearchEvent.HEALTH_CHECK_FAILED);

        config.configure(OpensearchState.PREPARED)
                .permit(OpensearchEvent.PROCESS_STARTED, OpensearchState.STARTING)
                .permit(OpensearchEvent.PROCESS_TERMINATED, OpensearchState.TERMINATED)
                .permit(OpensearchEvent.PROCESS_STOPPED, OpensearchState.TERMINATED)
                .ignore(OpensearchEvent.HEALTH_CHECK_FAILED);

        // the process has started already, now we have to wait for a running OS and available REST api
        // the startupFailuresCounter keeps track of failed REST status calls and allow failures during the
        // startup period
        config.configure(OpensearchState.STARTING)
                .permitDynamic(OpensearchEvent.HEALTH_CHECK_FAILED,
                        () -> startupFailuresCounter.failedTooManyTimes() ? OpensearchState.FAILED : OpensearchState.STARTING,
                        startupFailuresCounter::increment)
                .permit(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.AVAILABLE)
                .permit(OpensearchEvent.PROCESS_STOPPED, OpensearchState.TERMINATED)
                .permit(OpensearchEvent.PROCESS_TERMINATED, OpensearchState.TERMINATED);

        // the process is running and responding to the REST status, it's available for any usage
        config.configure(OpensearchState.AVAILABLE)
                .onEntry(restFailureCounter::resetFailuresCounter)
                .onEntry(rebootCounter::resetFailuresCounter)
                .permitReentry(OpensearchEvent.HEALTH_CHECK_OK)
                .permit(OpensearchEvent.HEALTH_CHECK_FAILED, OpensearchState.NOT_RESPONDING)
                .permit(OpensearchEvent.PROCESS_STOPPED, OpensearchState.TERMINATED)
                .permit(OpensearchEvent.PROCESS_TERMINATED, OpensearchState.TERMINATED)
                .permit(OpensearchEvent.PROCESS_REMOVE, OpensearchState.REMOVING)
                .ignore(OpensearchEvent.PROCESS_STARTED);

        // if the REST api is not responding, we'll jump to this state and count how many times the failure
        // occurs. If it fails ttoo many times, we'll mark the process as FAILED
        config.configure(OpensearchState.NOT_RESPONDING)
                .permitDynamic(OpensearchEvent.HEALTH_CHECK_FAILED,
                        () -> restFailureCounter.failedTooManyTimes() ? OpensearchState.FAILED : OpensearchState.NOT_RESPONDING,
                        restFailureCounter::increment
                )
                .permit(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.AVAILABLE)
                .permit(OpensearchEvent.PROCESS_STOPPED, OpensearchState.TERMINATED)
                .permit(OpensearchEvent.PROCESS_TERMINATED, OpensearchState.TERMINATED);

        // failed and we see the process as not recoverable.
        // TODO: what to do if the process fails? Reboot?
        config.configure(OpensearchState.FAILED)
                .ignore(OpensearchEvent.HEALTH_CHECK_FAILED)
                .permit(OpensearchEvent.HEALTH_CHECK_OK, OpensearchState.AVAILABLE)
                .permit(OpensearchEvent.PROCESS_STOPPED, OpensearchState.TERMINATED)
                .permit(OpensearchEvent.PROCESS_TERMINATED, OpensearchState.TERMINATED);

        // final state, the process is not alive anymore, terminated on the operating system level
        config.configure(OpensearchState.TERMINATED)
                .permit(OpensearchEvent.PROCESS_STARTED, OpensearchState.STARTING, rebootCounter::increment)
                .ignore(OpensearchEvent.HEALTH_CHECK_FAILED)
                .ignore(OpensearchEvent.PROCESS_STOPPED)
                .ignore(OpensearchEvent.PROCESS_TERMINATED); // final state, all following terminate events are ignored

        config.configure(OpensearchState.REMOVING)
                .onEntry(process::remove)
                .ignore(OpensearchEvent.HEALTH_CHECK_OK)
                .permit(OpensearchEvent.HEALTH_CHECK_FAILED, OpensearchState.FAILED)
                .permit(OpensearchEvent.PROCESS_STOPPED, OpensearchState.REMOVED);

        config.configure(OpensearchState.REMOVED)
                .permit(OpensearchEvent.RESET, OpensearchState.WAITING_FOR_CONFIGURATION, process::reset)
                .ignore(OpensearchEvent.PROCESS_STOPPED);

        return new OpensearchStateMachine(OpensearchState.WAITING_FOR_CONFIGURATION, config);
    }

    public StateMachineTracerAggregator getTracerAggregator() {
        return tracerAggregator;
    }

    public void fire(OpensearchEvent trigger, OpensearchEvent errorEvent) {
        try {
            super.fire(trigger);
        } catch (Exception e) {
            log.error(e.getMessage());
            super.fire(errorEvent);
        }
    }

    @Override
    public void fire(OpensearchEvent trigger) {
        fire(trigger, OpensearchEvent.HEALTH_CHECK_FAILED);
    }
}
