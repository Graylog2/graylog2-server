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
package org.graylog.datanode.process;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;

public class ProcessStateMachine {

    /**
     * How many times can the OS rest api call fail before we switch to the failed state
     */
    public static final int MAX_REST_TEMPORARY_FAILURES = 3;
    public static final int MAX_REST_STARTUP_FAILURES = 5;
    public static final int MAX_REBOOT_FAILURES = 3;

    public static StateMachine<ProcessState, ProcessEvent> createNew() {

        final FailuresCounter restFailureCounter = FailuresCounter.oneBased(MAX_REST_TEMPORARY_FAILURES);
        final FailuresCounter startupFailuresCounter = FailuresCounter.oneBased(MAX_REST_STARTUP_FAILURES);
        final FailuresCounter rebootCounter = FailuresCounter.oneBased(MAX_REBOOT_FAILURES);

        StateMachineConfig<ProcessState, ProcessEvent> config = new StateMachineConfig<>();

        // Freshly created process, it hasn't started yet and doesn't have any pid.
        config.configure(ProcessState.WAITING_FOR_CONFIGURATION)
                .permit(ProcessEvent.PROCESS_PREPARED, ProcessState.PREPARED)
                // jump to started only allowed to facilitate startup with insecure config
                .permit(ProcessEvent.PROCESS_STARTED, ProcessState.STARTING)
                .ignore(ProcessEvent.HEALTH_CHECK_FAILED);

        config.configure(ProcessState.PREPARED)
                .permit(ProcessEvent.PROCESS_STARTED, ProcessState.STARTING)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED)
                .permit(ProcessEvent.PROCESS_STOPPED, ProcessState.TERMINATED)
                .ignore(ProcessEvent.HEALTH_CHECK_FAILED);

        // the process has started already, now we have to wait for a running OS and available REST api
        // the startupFailuresCounter keeps track of failed REST status calls and allow failures during the
        // startup period
        config.configure(ProcessState.STARTING)
                .permitDynamic(ProcessEvent.HEALTH_CHECK_FAILED,
                        () -> startupFailuresCounter.failedTooManyTimes() ? ProcessState.FAILED : ProcessState.STARTING,
                        startupFailuresCounter::increment)
                .permit(ProcessEvent.HEALTH_CHECK_OK, ProcessState.AVAILABLE)
                .permit(ProcessEvent.PROCESS_STOPPED, ProcessState.TERMINATED)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        // the process is running and responding to the REST status, it's available for any usage
        config.configure(ProcessState.AVAILABLE)
                .onEntry(restFailureCounter::resetFailuresCounter)
                .onEntry(rebootCounter::resetFailuresCounter)
                .permitReentry(ProcessEvent.HEALTH_CHECK_OK)
                .permit(ProcessEvent.HEALTH_CHECK_FAILED, ProcessState.NOT_RESPONDING)
                .permit(ProcessEvent.PROCESS_STOPPED, ProcessState.TERMINATED)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED)
                .permit(ProcessEvent.PROCESS_REMOVE, ProcessState.REMOVING)
                .ignore(ProcessEvent.PROCESS_STARTED);

        // if the REST api is not responding, we'll jump to this state and count how many times the failure
        // occurs. If it fails ttoo many times, we'll mark the process as FAILED
        config.configure(ProcessState.NOT_RESPONDING)
                .permitDynamic(ProcessEvent.HEALTH_CHECK_FAILED,
                        () -> restFailureCounter.failedTooManyTimes() ? ProcessState.FAILED : ProcessState.NOT_RESPONDING,
                        restFailureCounter::increment
                )
                .permit(ProcessEvent.HEALTH_CHECK_OK, ProcessState.AVAILABLE)
                .permit(ProcessEvent.PROCESS_STOPPED, ProcessState.TERMINATED)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        // failed and we see the process as not recoverable.
        // TODO: what to do if the process fails? Reboot?
        config.configure(ProcessState.FAILED)
                .ignore(ProcessEvent.HEALTH_CHECK_FAILED)
                .permit(ProcessEvent.HEALTH_CHECK_OK, ProcessState.AVAILABLE)
                .permit(ProcessEvent.PROCESS_STOPPED, ProcessState.TERMINATED)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        // final state, the process is not alive anymore, terminated on the operating system level
        config.configure(ProcessState.TERMINATED)
                .permit(ProcessEvent.PROCESS_STARTED, ProcessState.STARTING, rebootCounter::increment)
                .ignore(ProcessEvent.HEALTH_CHECK_FAILED)
                .ignore(ProcessEvent.PROCESS_STOPPED)
                .ignore(ProcessEvent.PROCESS_TERMINATED); // final state, all following terminate events are ignored

        config.configure(ProcessState.REMOVING)
                .ignore(ProcessEvent.HEALTH_CHECK_OK)
                .permit(ProcessEvent.HEALTH_CHECK_FAILED, ProcessState.FAILED)
                .permit(ProcessEvent.PROCESS_STOPPED, ProcessState.REMOVED);

        config.configure(ProcessState.REMOVED)
                .permit(ProcessEvent.RESET, ProcessState.WAITING_FOR_CONFIGURATION)
                .ignore(ProcessEvent.PROCESS_STOPPED);

        return new StateMachine<>(ProcessState.WAITING_FOR_CONFIGURATION, config);
    }
}
