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
package org.graylog.plugins.pipelineprocessor.functions.arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

public class ArrayContains extends AbstractArrayFunction<Boolean> {
    public static final String NAME = "array_contains";

    private final ParameterDescriptor<Object, List> elementsParam;
    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> caseSensitiveParam;
    private final ObjectMapper objectMapper;

    @Inject
    public ArrayContains(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        elementsParam = ParameterDescriptor.type("elements", Object.class, List.class)
                .transform(AbstractArrayFunction::toList)
                .description("The input array, may be null")
                .build();
        valueParam = ParameterDescriptor.object("value")
                .description("The input value").build();
        caseSensitiveParam = ParameterDescriptor.bool("case_sensitive")
                .optional()
                .description("Whether or not to ignore case when checking string arrays").build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final List<Object> elements = elementsParam.optional(args, context).orElse(Collections.emptyList());
        final Object value = valueParam.required(args, context);
        final boolean caseSensitive = caseSensitiveParam.optional(args, context).orElse(false);

        if (elements.isEmpty()) {
            return false;
        }

        if (!caseSensitive && containsStringValue(elements)) {
            return elements.stream()
                    .anyMatch(e -> e.toString().equalsIgnoreCase(String.valueOf(value)));
        }

        return arrayContains(elements, value);
    }

    private boolean arrayContains(List<Object> elements, Object value) {
        for (Object element : elements.stream().filter(e -> !(e instanceof NullNode)).toList()) {
            if (element instanceof IntNode || element instanceof LongNode) {
                /* Allow ints and longs to be compared. Sometimes the array will contain ints,
                 * but a single number passed as the value will be typed as a long.
                 */
                final Number number = ((NumericNode) element).numberValue();
                if (value.equals(number.intValue())) {
                    return true;
                } else if (value.equals(number.longValue())) {
                    return true;
                }
            } else if (element instanceof JsonNode) {
                // Extract embedded JsonNode elements for comparison.
                if (objectMapper.convertValue(element, Object.class).equals(value)) {
                    return true;
                }
            }
        }
        return elements.contains(value);
    }

    private static boolean containsStringValue(List elements) {
        return elements.get(0) instanceof String;
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .pure(true)
                .returnType(Boolean.class)
                .params(ImmutableList.of(elementsParam, valueParam, caseSensitiveParam))
                .description("Checks if the specified element is contained in the array.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Check if array contains value")
                .ruleBuilderTitle("Check if '${value}' is contained in array '${elements}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.ARRAY)
                .build();
    }
}
