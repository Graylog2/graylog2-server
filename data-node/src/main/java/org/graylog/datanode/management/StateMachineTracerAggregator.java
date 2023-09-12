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

import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.StateMachineTracer;

import java.util.LinkedList;
import java.util.List;

public class StateMachineTracerAggregator implements StateMachineTracer {

    private final List<StateMachineTracer> delegates = new LinkedList<>();

    public void addTracer(StateMachineTracer tracer) {
        delegates.add(tracer);
    }

    public void removeTracer(StateMachineTracer tracer) {
        delegates.remove(tracer);
    }

    @Override
    public void trigger(ProcessEvent processEvent) {
        delegates.forEach(d -> d.trigger(processEvent));
    }

    @Override
    public void transition(ProcessEvent processEvent, ProcessState s1, ProcessState s2) {
        delegates.forEach(d -> d.transition(processEvent, s1, s2));
    }
}
