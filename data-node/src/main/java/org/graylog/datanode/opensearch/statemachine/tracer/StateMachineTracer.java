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

import com.github.oxo42.stateless4j.delegates.Trace;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;

/**
 * The tracer allows to observe triggered event (before) and transitions (after) of the {@link OpensearchStateMachine}
 */
public interface StateMachineTracer extends Trace<OpensearchState, OpensearchEvent> {

    default void setStateMachine(OpensearchStateMachine stateMachine) {
    }

}
