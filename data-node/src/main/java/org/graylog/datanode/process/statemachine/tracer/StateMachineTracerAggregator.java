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
package org.graylog.datanode.process.statemachine.tracer;

import java.util.LinkedList;
import java.util.List;

public class StateMachineTracerAggregator<STATE, EVENT> implements StateMachineTracer<STATE, EVENT> {

    private final List<StateMachineTracer<STATE, EVENT>> delegates = new LinkedList<>();

    public void addTracer(StateMachineTracer<STATE, EVENT> tracer) {
        delegates.add(tracer);
    }

    public void removeTracer(StateMachineTracer<STATE, EVENT> tracer) {
        delegates.remove(tracer);
    }

    @Override
    public void trigger(EVENT processEvent) {
        delegates.forEach(d -> d.trigger(processEvent));
    }

    @Override
    public void transition(EVENT processEvent, STATE s1, STATE s2) {
        delegates.forEach(d -> d.transition(processEvent, s1, s2));
    }

}
