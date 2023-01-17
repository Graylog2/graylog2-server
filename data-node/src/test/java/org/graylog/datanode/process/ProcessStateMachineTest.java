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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProcessStateMachineTest {

    @Test
    void testOptimalScenario() {
        final StateMachine<ProcessState, ProcessEvent> machine = ProcessStateMachine.createNew();
        Assertions.assertEquals(machine.getState(), ProcessState.NEW);

        machine.fire(ProcessEvent.PROCESS_STARTED);
        Assertions.assertEquals(ProcessState.RUNNING, machine.getState());

        machine.fire(ProcessEvent.HEALTH_CHECK_GREEN);
        Assertions.assertEquals(ProcessState.AVAILABLE, machine.getState());

        machine.fire(ProcessEvent.PROCESS_TERMINATED);
        Assertions.assertEquals(ProcessState.TERMINATED, machine.getState());
    }

    @Test
    void testRestFailing() {
        final StateMachine<ProcessState, ProcessEvent> machine = ProcessStateMachine.createNew();
        Assertions.assertEquals(machine.getState(), ProcessState.NEW);

        machine.fire(ProcessEvent.PROCESS_STARTED);
        Assertions.assertEquals(ProcessState.RUNNING, machine.getState());

        machine.fire(ProcessEvent.HEALTH_CHECK_GREEN);
        Assertions.assertEquals(ProcessState.AVAILABLE, machine.getState());
        machine.fire(ProcessEvent.HEALTH_CHECK_FAILED);
        machine.fire(ProcessEvent.HEALTH_CHECK_FAILED);
        machine.fire(ProcessEvent.HEALTH_CHECK_FAILED);

        // three failures are still accepted
        Assertions.assertEquals(ProcessState.FAILING, machine.getState());

        // fourth should bring the state to FAILED
        machine.fire(ProcessEvent.HEALTH_CHECK_FAILED);
        Assertions.assertEquals(ProcessState.FAILED, machine.getState());

        machine.fire(ProcessEvent.HEALTH_CHECK_GREEN);
        Assertions.assertEquals(ProcessState.AVAILABLE, machine.getState());
    }
}
