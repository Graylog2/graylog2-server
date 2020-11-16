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
package org.graylog.plugins.pipelineprocessor.processors.listeners;

import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog2.plugin.Message;

import java.util.Set;

public interface InterpreterListener {
    void startProcessing();
    void finishProcessing();
    void processStreams(Message message, Set<Pipeline> pipelines, Set<String> streams);
    void enterStage(Stage stage);
    void exitStage(Stage stage);
    void evaluateRule(Rule rule, Pipeline pipeline);
    void failEvaluateRule(Rule rule, Pipeline pipeline);
    void satisfyRule(Rule rule, Pipeline pipeline);
    void dissatisfyRule(Rule rule, Pipeline pipeline);
    void executeRule(Rule rule, Pipeline pipeline);
    void finishExecuteRule(Rule rule, Pipeline pipeline);
    void failExecuteRule(Rule rule, Pipeline pipeline);
    void continuePipelineExecution(Pipeline pipeline, Stage stage);
    void stopPipelineExecution(Pipeline pipeline, Stage stage);
}
