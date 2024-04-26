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
package org.graylog.datanode.opensearch;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.delegates.Trace;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.graylog.datanode.process.ManagableProcess;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestableOpensearchProcess implements ManagableProcess<String, OpensearchEvent, OpensearchState> {

    private static final Logger LOG = LoggerFactory.getLogger(TestableOpensearchProcess.class);

    private final StateMachine<OpensearchState, OpensearchEvent> stateMachine;

    public TestableOpensearchProcess(OpensearchProcess opensearchProcess) {
        this.stateMachine = OpensearchStateMachine.createNew(opensearchProcess);
    }

    @Override
    public void configure(String ignored) {
        LOG.debug("Preparing process");
        onEvent(OpensearchEvent.PROCESS_PREPARED);
    }

    @Override
    public void start() {
        LOG.debug("Starting process");
        onEvent(OpensearchEvent.PROCESS_STARTED);
    }

    @Override
    public void stop() {
        LOG.debug("Stopping process process");
        onEvent(OpensearchEvent.PROCESS_STOPPED);
    }

    @Override
    public void onEvent(OpensearchEvent event) {
        stateMachine.fire(event);
    }

    @Override
    public void addStateMachineTracer(Trace<OpensearchState, OpensearchEvent> stateMachineTracer) {
        stateMachine.setTrace(stateMachineTracer);
    }

    @Override
    public boolean isInState(OpensearchState state) {
        return this.stateMachine.isInState(state);
    }
}
