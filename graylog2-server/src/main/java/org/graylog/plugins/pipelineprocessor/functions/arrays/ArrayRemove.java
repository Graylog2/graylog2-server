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

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArrayRemove extends AbstractArrayFunction<List> {
    public static final String NAME = "array_remove";
    private static final Logger LOG = LoggerFactory.getLogger(ArrayRemove.class);
    private static final String VALUE_MISSING_MESSAGE = "Value [{}] was not present in array.";

    private final ParameterDescriptor<Object, List> elementsParam;
    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> removeAllParam;

    public ArrayRemove() {
        elementsParam = ParameterDescriptor.type("elements", Object.class, List.class)
                .transform(AbstractArrayFunction::toList)
                .description("The input array, all must have the same data type, may be null")
                .build();
        valueParam = ParameterDescriptor.object("value")
                .description("The value to remove from the array").build();
        removeAllParam = ParameterDescriptor.bool("remove_all")
                .optional()
                .description("Whether or not to remove all elements, or just a single one").build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List evaluate(FunctionArgs args, EvaluationContext context) {
        // List must be copied to avoid mutating the source object.
        final List<Object> elements = new ArrayList<>(elementsParam.optional(args, context)
                .orElse(Collections.emptyList()));
        final Object value = valueParam.required(args, context);
        final boolean removeAll = removeAllParam.optional(args, context).orElse(false);

        if (elements.isEmpty()) {
            return Collections.emptyList();
        }

        if (removeAll) {
            if (!elements.removeAll(Collections.singleton(value))) {
                LOG.trace(VALUE_MISSING_MESSAGE, value);
            }
            return elements;
        }

        if (!elements.remove(value)) {
            LOG.trace(VALUE_MISSING_MESSAGE, value);
        }
        return elements;
    }

    @Override
    public FunctionDescriptor<List> descriptor() {
        return FunctionDescriptor.<List>builder()
                .name(NAME)
                .pure(true)
                .returnType(List.class)
                .params(ImmutableList.of(elementsParam, valueParam, removeAllParam))
                .description("Removes the specified element from the array.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Remove element from array")
                .ruleBuilderTitle("Remove '${value}' from array '${elements}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.ARRAY)
                .build();
    }
}
