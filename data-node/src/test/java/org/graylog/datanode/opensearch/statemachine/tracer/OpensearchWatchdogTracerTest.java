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
package org.graylog.datanode.opensearch.statemachine.tracer;

import org.graylog.datanode.opensearch.TestableOpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.junit.jupiter.api.Test;

// TODO: fix test / TestableOpenProcess
class OpensearchWatchdogTracerTest {


    @Test
    void testLifecycle() {
        final TestableOpensearchProcess process = new TestableOpensearchProcess();
//        final OpensearchWatchdog watchdog = new OpensearchWatchdog(process, 3);
//        process.addStateMachineTracer(watchdog);
//        process.configure("ignored");
//        process.start();
//
//        // both process and watchdog are running now. Let's stop the process and see if the watchdog will restart it
//        terminateProcess(process);
//
//        // see if the process is starting again
//        Assertions.assertThat(isInStartingState(process)).isTrue();
//
//        // repeat
//        terminateProcess(process);
//        Assertions.assertThat(isInStartingState(process)).isTrue();
//
//        terminateProcess(process);
//        Assertions.assertThat(isInStartingState(process)).isTrue();
//
//        // this is the 4th termination, we give up trying
//        terminateProcess(process);
//
//        Assertions.assertThat(watchdog.isActive()).isFalse();
//        Assertions.assertThat(process.isInState(OpensearchState.TERMINATED)).isTrue();
    }

    private void terminateProcess(TestableOpensearchProcess process) {
        process.onEvent(OpensearchEvent.PROCESS_TERMINATED);
    }

    private boolean isInStartingState(TestableOpensearchProcess process) {
        return process.isInState(OpensearchState.STARTING);
    }
}
