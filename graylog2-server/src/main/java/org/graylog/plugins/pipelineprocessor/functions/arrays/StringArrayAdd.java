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

import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StringArrayAdd extends AbstractFunction<List<String>> {
    public static final String NAME = "string_array_add";
    @SuppressWarnings("unchecked")
    private static final Class<List<String>> LIST_RETURN_TYPE = (Class<List<String>>) new TypeToken<List<String>>() {
    }.getRawType();

    private final ParameterDescriptor<Object, List<String>> elementsParam;
    private final ParameterDescriptor<Object, List<String>> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> onlyUniqueParam;

    public StringArrayAdd() {
        elementsParam = ParameterDescriptor.object("elements", LIST_RETURN_TYPE)
                .transform(this::transformToList)
                .description("The input string array, may be null")
                .build();
        valueParam = ParameterDescriptor.object("value", LIST_RETURN_TYPE)
                .transform(this::transformToList)
                .description("The string (or string array) value to add to the array").build();
        onlyUniqueParam = ParameterDescriptor.bool("only_unique")
                .optional()
                .description("Only add elements if not already present").build();
    }

    private List<String> transformToList(Object value) {
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).stream()
                    .map(StringArrayAdd::convertValue)
                    // Non-text JSON nodes will return null, and type conversions are intentionally not done,
                    // since only string inputs are supported.
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.singletonList(value.toString());
    }

    private static String convertValue(Object o) {
        if (o instanceof ValueNode node) {
            return node.textValue();
        }
        return o.toString();
    }

    @Override
    public List<String> evaluate(FunctionArgs args, EvaluationContext context) {
        List<String> elements = new ArrayList<String>(elementsParam.optional(args, context).orElse(Collections.emptyList()));
        final List<String> valueArray = valueParam.optional(args, context).orElse(Collections.emptyList());
        final boolean onlyUnique = onlyUniqueParam.optional(args, context).orElse(false);

        if (onlyUnique) {
            valueArray.stream().filter(v -> !elements.contains(v)).forEach(elements::add);
            return elements;
        }

        elements.addAll(valueArray.stream().map(Object::toString).toList());
        return elements;
    }

    @Override
    public FunctionDescriptor<List<String>> descriptor() {
        return FunctionDescriptor.<List<String>>builder()
                .name(NAME)
                .pure(true)
                .returnType(LIST_RETURN_TYPE)
                .params(ImmutableList.of(elementsParam, valueParam, onlyUniqueParam))
                .description("Adds the specified string (or string array) value to the supplied string array.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Add to array")
                .ruleBuilderTitle("Add '${elements}' to array '${value}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.ARRAY)
                .build();
    }
}
