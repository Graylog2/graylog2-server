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
package org.graylog.datanode.opensearch.statemachine;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.tracer.StateMachineTracer;
import org.graylog.datanode.opensearch.statemachine.tracer.StateMachineTracerAggregator;

import java.util.Set;

public class OpensearchStateMachineProvider implements Provider<OpensearchStateMachine> {
    private final OpensearchStateMachine opensearchStateMachine;

    @Inject
    public OpensearchStateMachineProvider(Set<StateMachineTracer> tracer, OpensearchProcess process) {
        this.opensearchStateMachine = OpensearchStateMachine.createNew(process);
        StateMachineTracerAggregator aggregator = opensearchStateMachine.getTracerAggregator();
        tracer.forEach(t -> {
            t.setStateMachine(opensearchStateMachine);
            aggregator.addTracer(t);
        });
    }

    @Override
    public OpensearchStateMachine get() {
        return opensearchStateMachine;
    }
}
