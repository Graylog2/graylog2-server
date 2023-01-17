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

import java.util.concurrent.atomic.AtomicInteger;

public class ProcessStateMachine {

    /**
     * How many times can the OS rest api call fail before we switch to the failed state
     */
    public static final int MAX_REST_TEMPORARY_FAILURES = 3;
    public static final int MAX_REST_STARTUP_FAILURES = 5;

    public static StateMachine<ProcessState, ProcessEvent> createNew() {

        final AtomicInteger failuresCounter = new AtomicInteger(0);
        final AtomicInteger startupFailuresCounter = new AtomicInteger(0);

        StateMachineConfig<ProcessState, ProcessEvent> config = new StateMachineConfig<>();

        config.configure(ProcessState.NEW)
                .permit(ProcessEvent.PROCESS_STARTED, ProcessState.STARTING);

        config.configure(ProcessState.STARTING)
                .onEntry(() -> incrementFailures(startupFailuresCounter))
                .permitDynamic(ProcessEvent.HEALTH_CHECK_FAILED,
                        () -> failedTooManyTimes(startupFailuresCounter, MAX_REST_STARTUP_FAILURES) ? ProcessState.FAILED : ProcessState.STARTING)
                .permit(ProcessEvent.HEALTH_CHECK_GREEN, ProcessState.AVAILABLE)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        config.configure(ProcessState.AVAILABLE)
                .onEntry(() -> resetFailuresCounter(failuresCounter))
                .permitReentry(ProcessEvent.HEALTH_CHECK_GREEN)
                .permit(ProcessEvent.HEALTH_CHECK_FAILED, ProcessState.NOT_RESPONDING)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        config.configure(ProcessState.NOT_RESPONDING)
                .onEntry(() -> incrementFailures(failuresCounter))
                .permitDynamic(ProcessEvent.HEALTH_CHECK_FAILED,
                        () -> failedTooManyTimes(failuresCounter, MAX_REST_TEMPORARY_FAILURES) ? ProcessState.FAILED : ProcessState.NOT_RESPONDING)
                .permit(ProcessEvent.HEALTH_CHECK_GREEN, ProcessState.AVAILABLE)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        config.configure(ProcessState.FAILED)
                .ignore(ProcessEvent.HEALTH_CHECK_FAILED)
                .permit(ProcessEvent.HEALTH_CHECK_GREEN, ProcessState.AVAILABLE)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        config.configure(ProcessState.TERMINATED)
                .ignore(ProcessEvent.PROCESS_TERMINATED); // final state, all following terminate events are ignored


        return new StateMachine<>(ProcessState.NEW, config);
    }

    private static void incrementFailures(AtomicInteger failuresCounter) {
        failuresCounter.incrementAndGet();
    }

    private static boolean failedTooManyTimes(AtomicInteger failuresCounter, int maxRetries) {
        return failuresCounter.get() >= maxRetries;
    }

    private static void resetFailuresCounter(AtomicInteger failuresCounter) {
        failuresCounter.set(0);
    }
}
