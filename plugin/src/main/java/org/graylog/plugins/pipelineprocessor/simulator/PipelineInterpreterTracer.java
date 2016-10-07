/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
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
