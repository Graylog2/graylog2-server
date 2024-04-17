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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DatanodeStateMachineTest {

    @Test
    void testOptimalScenario() {
        final StateMachine<DatanodeState, DatanodeEvent> machine = DatanodeStateMachine.createNew();
        Assertions.assertEquals(machine.getState(), DatanodeState.WAITING_FOR_CONFIGURATION);

        machine.fire(DatanodeEvent.PROCESS_PREPARED);
        Assertions.assertEquals(DatanodeState.PREPARED, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_STARTED);
        Assertions.assertEquals(DatanodeState.STARTING, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(DatanodeState.AVAILABLE, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_TERMINATED);
        Assertions.assertEquals(DatanodeState.TERMINATED, machine.getState());
    }

    @Test
    void testRestFailing() {
        final StateMachine<DatanodeState, DatanodeEvent> machine = DatanodeStateMachine.createNew();
        Assertions.assertEquals(machine.getState(), DatanodeState.WAITING_FOR_CONFIGURATION);

        machine.fire(DatanodeEvent.PROCESS_PREPARED);
        Assertions.assertEquals(DatanodeState.PREPARED, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_STARTED);
        Assertions.assertEquals(DatanodeState.STARTING, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(DatanodeState.AVAILABLE, machine.getState());
        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);

        // three failures are still accepted
        Assertions.assertEquals(DatanodeState.NOT_RESPONDING, machine.getState());

        // fourth should bring the state to FAILED
        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        Assertions.assertEquals(DatanodeState.FAILED, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(DatanodeState.AVAILABLE, machine.getState());
    }

    @Test
    void testStartupFailure() {
        final StateMachine<DatanodeState, DatanodeEvent> machine = DatanodeStateMachine.createNew();
        Assertions.assertEquals(machine.getState(), DatanodeState.WAITING_FOR_CONFIGURATION);

        machine.fire(DatanodeEvent.PROCESS_PREPARED);
        Assertions.assertEquals(DatanodeState.PREPARED, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_STARTED);
        Assertions.assertEquals(DatanodeState.STARTING, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        Assertions.assertEquals(DatanodeState.STARTING, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        // after five repetitions we give up waiting for the process start and fail
        Assertions.assertEquals(DatanodeState.FAILED, machine.getState());
    }

    @Test
    void testStartupFailureResolved() {
        final StateMachine<DatanodeState, DatanodeEvent> machine = DatanodeStateMachine.createNew();
        Assertions.assertEquals(machine.getState(), DatanodeState.WAITING_FOR_CONFIGURATION);

        machine.fire(DatanodeEvent.PROCESS_PREPARED);
        Assertions.assertEquals(DatanodeState.PREPARED, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_STARTED);
        Assertions.assertEquals(DatanodeState.STARTING, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        Assertions.assertEquals(DatanodeState.STARTING, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        machine.fire(DatanodeEvent.HEALTH_CHECK_OK);
        // succeeded just in time before we give up
        Assertions.assertEquals(DatanodeState.AVAILABLE, machine.getState());
    }

    @Test
    void testSuccessfullRemoval() {
        final StateMachine<DatanodeState, DatanodeEvent> machine = DatanodeStateMachine.createNew();
        Assertions.assertEquals(machine.getState(), DatanodeState.WAITING_FOR_CONFIGURATION);

        machine.fire(DatanodeEvent.PROCESS_PREPARED);
        Assertions.assertEquals(DatanodeState.PREPARED, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_STARTED);
        Assertions.assertEquals(DatanodeState.STARTING, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(DatanodeState.AVAILABLE, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_REMOVE);
        Assertions.assertEquals(DatanodeState.REMOVING, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_STOPPED);
        Assertions.assertEquals(DatanodeState.REMOVED, machine.getState());

    }

    @Test
    void testFailingRemoval() {
        final StateMachine<DatanodeState, DatanodeEvent> machine = DatanodeStateMachine.createNew();
        Assertions.assertEquals(machine.getState(), DatanodeState.WAITING_FOR_CONFIGURATION);

        machine.fire(DatanodeEvent.PROCESS_PREPARED);
        Assertions.assertEquals(DatanodeState.PREPARED, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_STARTED);
        Assertions.assertEquals(DatanodeState.STARTING, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_OK);
        Assertions.assertEquals(DatanodeState.AVAILABLE, machine.getState());

        machine.fire(DatanodeEvent.PROCESS_REMOVE);
        Assertions.assertEquals(DatanodeState.REMOVING, machine.getState());

        machine.fire(DatanodeEvent.HEALTH_CHECK_FAILED);
        Assertions.assertEquals(DatanodeState.FAILED, machine.getState());

    }

}
