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

public class NoopInterpreterListener implements InterpreterListener {
    @Override
    public void startProcessing() {

    }

    @Override
    public void finishProcessing() {

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
    public void finishExecuteRule(Rule rule, Pipeline pipeline) {

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
