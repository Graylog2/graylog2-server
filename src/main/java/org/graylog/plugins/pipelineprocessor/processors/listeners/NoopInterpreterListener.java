package org.graylog.plugins.pipelineprocessor.processors.listeners;

import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog2.plugin.Message;

import java.util.Set;

public class NoopInterpreterListener implements InterpreterListener {
    @Override
    public void startProcessing() {

    }

    @Override
    public void finishProcessing() {

    }

    @Override
    public void processDefaultStream(Message messageId, Set<Pipeline> pipelines) {

    }

    @Override
    public void processStreams(Message messageId, Set<Pipeline> pipelines, Set<String> streams) {

    }

    @Override
    public void enterStage(Stage stage) {

    }

    @Override
    public void exitStage(Stage stage) {

    }

    @Override
    public void evaluateRule(Rule rule, Pipeline pipeline) {

    }

    @Override
    public void failEvaluateRule(Rule rule, Pipeline pipeline) {

    }

    @Override
    public void satisfyRule(Rule rule, Pipeline pipeline) {

    }

    @Override
    public void dissatisfyRule(Rule rule, Pipeline pipeline) {

    }

    @Override
    public void executeRule(Rule rule, Pipeline pipeline) {

    }

    @Override
    public void failExecuteRule(Rule rule, Pipeline pipeline) {

    }

    @Override
    public void continuePipelineExecution(Pipeline pipeline, Stage stage) {

    }

    @Override
    public void stopPipelineExecution(Pipeline pipeline, Stage stage) {

    }
}
