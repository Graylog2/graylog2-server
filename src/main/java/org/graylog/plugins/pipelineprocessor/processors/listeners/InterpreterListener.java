package org.graylog.plugins.pipelineprocessor.processors.listeners;

import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog2.plugin.Message;

import java.util.Set;

public interface InterpreterListener {
    void startProcessing();
    void finishProcessing();
    void processDefaultStream(Message message, Set<Pipeline> pipelines);
    void processStreams(Message message, Set<Pipeline> pipelines, Set<String> streams);
    void enterStage(Stage stage);
    void exitStage(Stage stage);
    void evaluateRule(Rule rule, Pipeline pipeline);
    void failEvaluateRule(Rule rule, Pipeline pipeline);
    void satisfyRule(Rule rule, Pipeline pipeline);
    void dissatisfyRule(Rule rule, Pipeline pipeline);
    void executeRule(Rule rule, Pipeline pipeline);
    void failExecuteRule(Rule rule, Pipeline pipeline);
    void continuePipelineExecution(Pipeline pipeline, Stage stage);
    void stopPipelineExecution(Pipeline pipeline, Stage stage);
}
