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
package org.graylog.plugins.pipelineprocessor.functions.array;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArrayAdd extends AbstractFunction<List> {
    public static final String NAME = "array_add";
    private static final Logger LOG = LoggerFactory.getLogger(ArrayAdd.class);

    private final ParameterDescriptor<Object, List> elementsParam;
    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> onlyUniqueParam;


    public ArrayAdd() {
        elementsParam = ParameterDescriptor.type("elements", Object.class, List.class)
                .transform(ArrayAdd::toList)
                .description("The input array, all must have the same data type, may be null")
                .build();
        valueParam = ParameterDescriptor.object("value").description("The value to remove from the array").build();
        onlyUniqueParam = ParameterDescriptor.bool("case_sensitive")
                .optional()
                .description("Only add element if now already present").build();
    }

    @SuppressWarnings("rawtypes")
    private static List toList(Object obj) {
        if (obj instanceof Collection) {
            return new ArrayList((Collection) obj);
        } else {
            throw new IllegalArgumentException("Unsupported data type for parameter 'elements': " + obj.getClass().getCanonicalName());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List evaluate(FunctionArgs args, EvaluationContext context) {
        final List<Object> elements = elementsParam.optional(args, context).orElse(Collections.emptyList());
        final Object value = valueParam.required(args, context);
        final boolean onlyUnique = onlyUniqueParam.optional(args, context).orElse(false);

        if (elements.isEmpty()) {
            return Collections.emptyList();
        }

        if (onlyUnique && elements.contains(value)) {
            return elements;
        }

        if (elements.add(value) && LOG.isTraceEnabled()) {
            LOG.trace("Value [{}] was already present in array.", value);
        }
        return elements;
    }

    @Override
    public FunctionDescriptor<List> descriptor() {
        return FunctionDescriptor.<List>builder()
                .name(NAME)
                .pure(true)
                .returnType(List.class)
                .params(ImmutableList.of(elementsParam, valueParam, onlyUniqueParam))
                .description("Adds the specified element to the array.")
                .build();
    }
}
