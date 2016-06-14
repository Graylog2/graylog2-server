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
