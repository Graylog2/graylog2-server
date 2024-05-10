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
package org.graylog.plugins.pipelineprocessor.parser;

import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.graylog.plugins.pipelineprocessor.parser.RuleContentType.GL_PIPELINE_LANGUAGE;

public class PipelineRuleParser {
    private static AtomicLong uniqueId = new AtomicLong(0);

    private final PipelineLanguageRuleParser pipelineLanguageRuleParser;
    private final JavaScriptRuleParser javaScriptRuleParser;

    @Inject
    public PipelineRuleParser(FunctionRegistry functionRegistry) {
        this.pipelineLanguageRuleParser = new PipelineLanguageRuleParser(functionRegistry);
        this.javaScriptRuleParser = new JavaScriptRuleParser();
    }

    public Rule parseRule(String rule, boolean silent) throws ParseException {
        return parseRule(GL_PIPELINE_LANGUAGE, rule, silent);
    }

    public Rule parseRule(String id, String rule, boolean silent) throws ParseException {
        return parseRule(GL_PIPELINE_LANGUAGE, id, rule, silent);
    }

    public Rule parseRule(RuleContentType type, String rule, boolean silent) throws ParseException {
        return parseRule(type, "dummy" + uniqueId.getAndIncrement(), rule, silent);
    }

    public Rule parseRule(RuleContentType type, String id, String rule, boolean silent) throws ParseException {
        return switch (type) {
            case GL_PIPELINE_LANGUAGE -> pipelineLanguageRuleParser.parseRule(id, rule, silent);
            case JAVASCRIPT_MODULE -> javaScriptRuleParser.parseRule(id, rule, silent);
        };
    }

    public Pipeline parsePipeline(String id, String source) {
        return pipelineLanguageRuleParser.parsePipeline(id, source);
    }

    public List<Pipeline> parsePipelines(String pipelines) throws ParseException {
        return pipelineLanguageRuleParser.parsePipelines(pipelines);
    }
}
