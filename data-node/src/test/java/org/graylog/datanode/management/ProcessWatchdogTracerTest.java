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
package org.graylog.datanode.management;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.junit.jupiter.api.Test;

class ProcessWatchdogTracerTest {


    @Test
    void testLifecycle() {
        final TestableProcess process = new TestableProcess();
        final ProcessWatchdog watchdog = new ProcessWatchdog(process, 3);
        process.addStateMachineTracer(watchdog);
        process.configure("ignored");
        process.start();

        // both process and watchdog are running now. Let's stop the process and see if the watchdog will restart it
        terminateProcess(process);

        // see if the process is starting again
        Assertions.assertThat(isInStartingState(process)).isTrue();

        // repeat
        terminateProcess(process);
        Assertions.assertThat(isInStartingState(process)).isTrue();

        terminateProcess(process);
        Assertions.assertThat(isInStartingState(process)).isTrue();

        // this is the 4th termination, we give up trying
        terminateProcess(process);

        Assertions.assertThat(watchdog.isActive()).isFalse();
        Assertions.assertThat(process.isInState(ProcessState.TERMINATED)).isTrue();
    }

    private void terminateProcess(TestableProcess process) {
        process.onEvent(ProcessEvent.PROCESS_TERMINATED);
    }

    private boolean isInStartingState(TestableProcess process) {
        return process.isInState(ProcessState.STARTING);
    }
}
