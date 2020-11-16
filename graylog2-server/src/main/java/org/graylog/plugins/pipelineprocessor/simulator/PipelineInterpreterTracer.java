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
package org.graylog.plugins.pipelineprocessor.simulator;

import com.google.common.base.Stopwatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PipelineInterpreterTracer {
    private final List<PipelineInterpreterTrace> executionTrace;
    private final Stopwatch timer;
    private final SimulatorInterpreterListener simulatorInterpreterListener;

    public PipelineInterpreterTracer() {
        executionTrace = new ArrayList<>();
        timer = Stopwatch.createUnstarted();
        simulatorInterpreterListener = new SimulatorInterpreterListener(this);
    }

    public SimulatorInterpreterListener getSimulatorInterpreterListener() {
        return simulatorInterpreterListener;
    }

    public List<PipelineInterpreterTrace> getExecutionTrace() {
        return executionTrace;
    }

    public long took() {
        return timer.elapsed(TimeUnit.MICROSECONDS);
    }

    public void addTrace(String message) {
        executionTrace.add(PipelineInterpreterTrace.create(timer.elapsed(TimeUnit.MICROSECONDS), message));
    }

    public void startProcessing(String message) {
        timer.start();
        addTrace(message);
    }

    public void finishProcessing(String message) {
        timer.stop();
        addTrace(message);
    }
}
