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
package org.graylog.datanode.process.statemachine;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import org.graylog.datanode.process.statemachine.tracer.StateMachineTracer;
import org.graylog.datanode.process.statemachine.tracer.StateMachineTracerAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public abstract class ProcessStateMachine<STATE, EVENT> extends StateMachine<STATE, EVENT> {

    private final Logger LOG = LoggerFactory.getLogger(ProcessStateMachine.class);

    private final StateMachineTracerAggregator<STATE, EVENT> tracerAggregator = new StateMachineTracerAggregator<>();

    public ProcessStateMachine(STATE initialState,
                               StateMachineConfig<STATE, EVENT> config,
                               Set<StateMachineTracer<STATE, EVENT>> tracer) {
        super(initialState, config);
        addTracer(tracer);
        setTrace(tracerAggregator);
    }

    private void addTracer(Set<StateMachineTracer<STATE, EVENT>> tracers) {
        tracers.forEach(tracer -> {
            tracer.setStateMachine(this);
            tracerAggregator.addTracer(tracer);
        });
    }

    protected abstract EVENT getErrorEvent();

    private void fire(EVENT trigger, EVENT errorEvent) {
        try {
            super.fire(trigger);
        } catch (Exception e) {
            LOG.error("Failed to fire event " + trigger, e);
            super.fire(errorEvent);
        }
    }

    @Override
    public void fire(EVENT trigger) {
        fire(trigger, getErrorEvent());
    }

}
