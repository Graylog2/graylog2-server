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

import com.google.common.reflect.TypeToken;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.Match;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.grok.GrokPatternRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.of;

public class MultiGrokMatch extends AbstractFunction<GrokMatch.GrokResult> {
    public static final String NAME = "multi_grok";

    @SuppressWarnings("unchecked")
    private static final Class<List<String>> LIST_RETURN_TYPE = (Class<List<String>>) new TypeToken<List<String>>() {
    }.getRawType();
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<Object, List<String>> patternsParam;
    private final ParameterDescriptor<Boolean, Boolean> namedOnly;

    private final GrokPatternRegistry grokPatternRegistry;

    @Inject
    public MultiGrokMatch(GrokPatternRegistry grokPatternRegistry) {
        this.grokPatternRegistry = grokPatternRegistry;

        valueParam = ParameterDescriptor.string("value").description("The string to apply each Grok pattern against").build();
        patternsParam = ParameterDescriptor.object("patterns", LIST_RETURN_TYPE)
                .description("The Grok patterns to match in order")
                .transform(this::transformToList)
                .build();
        namedOnly = ParameterDescriptor.bool("only_named_captures").optional().description("Whether to only use explicitly named groups in the patterns").build();
    }

    @Override
    public GrokMatch.GrokResult evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final List<String> patterns = patternsParam.required(args, context);
        final boolean onlyNamedCaptures = namedOnly.optional(args, context).orElse(false);

        if (value == null || patterns == null || patterns.isEmpty()) {
            return null;
        }

        for (String pattern : patterns) {
            final Grok grok = grokPatternRegistry.cachedGrokForPattern(pattern, onlyNamedCaptures);

            final Match match = grok.match(value);
            if (!match.isNull()) {
                return new GrokMatch.GrokResult(match.captureFlattened());
            }
        }
        return new GrokMatch.GrokResult(Map.of());
    }

    private List<String> transformToList(Object value) {
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).stream()
                    .map(Object::toString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public FunctionDescriptor<GrokMatch.GrokResult> descriptor() {
        return FunctionDescriptor.<GrokMatch.GrokResult>builder()
                .name(NAME)
                .returnType(GrokMatch.GrokResult.class)
                .params(of(patternsParam, valueParam, namedOnly))
                .description("Applies a list of Grok patterns to a string and returns the first match")
                .build();
    }

}
