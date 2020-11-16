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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import com.google.common.collect.ForwardingMap;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.Match;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.grok.GrokPatternRegistry;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;

public class GrokMatch extends AbstractFunction<GrokMatch.GrokResult> {

    public static final String NAME = "grok";

    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> patternParam;
    private final ParameterDescriptor<Boolean, Boolean> namedOnly;

    private final GrokPatternRegistry grokPatternRegistry;

    @Inject
    public GrokMatch(GrokPatternRegistry grokPatternRegistry) {
        this.grokPatternRegistry = grokPatternRegistry;

        valueParam = ParameterDescriptor.string("value").description("The string to apply the Grok pattern against").build();
        patternParam = ParameterDescriptor.string("pattern").description("The Grok pattern").build();
        namedOnly = ParameterDescriptor.bool("only_named_captures").optional().description("Whether to only use explicitly named groups in the patterns").build();
    }

    @Override
    public GrokResult evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final String pattern = patternParam.required(args, context);
        final boolean onlyNamedCaptures = namedOnly.optional(args, context).orElse(false);

        if (value == null || pattern == null) {
            return null;
        }

        final Grok grok = grokPatternRegistry.cachedGrokForPattern(pattern, onlyNamedCaptures);

        final Match match = grok.match(value);;
        return new GrokResult(match.captureFlattened());
    }

    @Override
    public FunctionDescriptor<GrokResult> descriptor() {
        return FunctionDescriptor.<GrokResult>builder()
                .name(NAME)
                .returnType(GrokResult.class)
                .params(of(patternParam, valueParam, namedOnly))
                .description("Applies a Grok pattern to a string")
                .build();
    }

    public static class GrokResult extends ForwardingMap<String, Object> {
        private final Map<String, Object> captures;

        public GrokResult(Map<String, Object> captures) {
            this.captures = captures;
        }

        @Override
        protected Map<String, Object> delegate() {
            return captures;
        }

        public boolean isMatches() {
            return captures.size() > 0;
        }
    }
}
