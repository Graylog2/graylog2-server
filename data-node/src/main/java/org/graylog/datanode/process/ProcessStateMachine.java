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

    public static StateMachine<ProcessState, ProcessEvent> createNew() {

        final AtomicInteger failuresCounter = new AtomicInteger(0);

        StateMachineConfig<ProcessState, ProcessEvent> config = new StateMachineConfig<>();

        config.configure(ProcessState.NEW)
                .permit(ProcessEvent.PROCESS_STARTED, ProcessState.RUNNING);

        config.configure(ProcessState.RUNNING)
                .permit(ProcessEvent.HEALTH_CHECK_GREEN, ProcessState.AVAILABLE)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        config.configure(ProcessState.AVAILABLE)
                .onEntry(() -> resetFailuresCounter(failuresCounter))
                .permit(ProcessEvent.HEALTH_CHECK_FAILED, ProcessState.FAILING)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        config.configure(ProcessState.FAILING)
                .onEntry(() -> incrementFailures(failuresCounter))
                .permitDynamic(ProcessEvent.HEALTH_CHECK_FAILED,
                        () -> failedTooManyTimes(failuresCounter) ? ProcessState.FAILED : ProcessState.FAILING)
                .permit(ProcessEvent.HEALTH_CHECK_GREEN, ProcessState.AVAILABLE)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        config.configure(ProcessState.FAILED)
                .permit(ProcessEvent.HEALTH_CHECK_GREEN, ProcessState.AVAILABLE)
                .permit(ProcessEvent.PROCESS_TERMINATED, ProcessState.TERMINATED);

        config.configure(ProcessState.TERMINATED)
                .ignore(ProcessEvent.PROCESS_TERMINATED); // final state, all following terminate events are ignored


        return new StateMachine<>(ProcessState.NEW, config);
    }

    private static void incrementFailures(AtomicInteger failuresCounter) {
        failuresCounter.incrementAndGet();
    }

    private static boolean failedTooManyTimes(AtomicInteger failuresCounter) {
        return failuresCounter.get() >= MAX_REST_TEMPORARY_FAILURES;
    }

    private static void resetFailuresCounter(AtomicInteger failuresCounter) {
        failuresCounter.set(0);
    }
}
