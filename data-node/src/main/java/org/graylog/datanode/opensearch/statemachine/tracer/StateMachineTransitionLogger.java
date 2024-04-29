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

import jakarta.inject.Inject;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateMachineTransitionLogger implements StateMachineTracer {

    private static final Logger LOG = LoggerFactory.getLogger(StateMachineTransitionLogger.class);

    @Inject
    public StateMachineTransitionLogger() {
    }

    @Override
    public void trigger(OpensearchEvent trigger) {

    }

    @Override
    public void transition(OpensearchEvent trigger, OpensearchState source, OpensearchState destination) {
        if (!source.equals(destination)) {
            LOG.debug("Triggered {}, source state: {}, destination: {}", trigger, source, destination);
        }
    }

    @Override
    public void setStateMachine(OpensearchStateMachine stateMachine) {
        
    }
}
