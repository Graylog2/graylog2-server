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

import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.processors.listeners.InterpreterListener;
import org.graylog2.plugin.Message;

import java.util.Set;

class SimulatorInterpreterListener implements InterpreterListener {
    private final PipelineInterpreterTracer executionTrace;

    SimulatorInterpreterListener(PipelineInterpreterTracer executionTrace) {
        this.executionTrace = executionTrace;
    }

    @Override
    public void startProcessing() {
        executionTrace.startProcessing("Starting message processing");
    }

    @Override
    public void finishProcessing() {
        executionTrace.finishProcessing("Finished message processing");
    }

    @Override
    public void processStreams(Message message, Set<Pipeline> pipelines, Set<String> streams) {
        executionTrace.addTrace("Message " + message.getId() + " running " + pipelines + " for streams " + streams);
    }

    @Override
    public void enterStage(Stage stage) {
        executionTrace.addTrace("Enter " + stage);
    }

    @Override
    public void exitStage(Stage stage) {
        executionTrace.addTrace("Exit " + stage);
    }

    @Override
    public void evaluateRule(Rule rule, Pipeline pipeline) {
        executionTrace.addTrace("Evaluate " + rule + " in " + pipeline);
    }

    @Override
    public void failEvaluateRule(Rule rule, Pipeline pipeline) {
        executionTrace.addTrace("Failed evaluation " + rule + " in " + pipeline);
    }

    @Override
    public void satisfyRule(Rule rule, Pipeline pipeline) {
        executionTrace.addTrace("Evaluation satisfied " + rule + " in " + pipeline);
    }

    @Override
    public void dissatisfyRule(Rule rule, Pipeline pipeline) {
        executionTrace.addTrace("Evaluation not satisfied " + rule + " in " + pipeline);
    }

    @Override
    public void executeRule(Rule rule, Pipeline pipeline) {
        executionTrace.addTrace("Execute " + rule + " in " + pipeline);
    }

    @Override
    public void failExecuteRule(Rule rule, Pipeline pipeline) {
        executionTrace.addTrace("Failed execution " + rule + " in " + pipeline);
    }

    @Override
    public void continuePipelineExecution(Pipeline pipeline, Stage stage) {
        executionTrace.addTrace("Completed " + stage + " for " + pipeline + ", continuing to next stage");
    }

    @Override
    public void stopPipelineExecution(Pipeline pipeline, Stage stage) {
        executionTrace.addTrace("Completed " + stage + " for " + pipeline + ", NOT continuing to next stage");
    }
}
