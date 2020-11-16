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

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Join extends AbstractFunction<String> {
    public static final String NAME = "join";

    private final ParameterDescriptor<String, String> delimiterParam;
    private final ParameterDescriptor<Object, List> elementsParam;
    private final ParameterDescriptor<Long, Integer> startIndexParam;
    private final ParameterDescriptor<Long, Integer> endIndexParam;

    public Join() {
        elementsParam = ParameterDescriptor.type("elements", Object.class, List.class)
                .transform(Join::toList)
                .description("The list of strings to join together, may be null")
                .build();
        delimiterParam = ParameterDescriptor.string("delimiter").optional()
                .description("The delimiter that separates each element. Default: none")
                .build();
        startIndexParam = ParameterDescriptor.integer("start", Integer.class).optional()
                .transform(Ints::saturatedCast)
                .description("The first index to start joining from. It is an error to pass in an index larger than the number of elements")
                .build();
        endIndexParam = ParameterDescriptor.integer("end", Integer.class).optional()
                .transform(Ints::saturatedCast)
                .description("The index to stop joining from (exclusive). It is an error to pass in an index larger than the number of elements")
                .build();
    }

    private static List toList(Object obj) {
        if (obj instanceof Collection) {
            return ImmutableList.copyOf((Collection) obj);
        } else {
            throw new IllegalArgumentException("Unsupported data type for parameter 'elements': " + obj.getClass().getCanonicalName());
        }
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final List elements = elementsParam.optional(args, context).orElse(Collections.emptyList());
        final int length = elements.size();

        final String delimiter = delimiterParam.required(args, context);
        final int startIndex = startIndexParam.optional(args, context).filter(idx -> idx >= 0).orElse(0);
        final int endIndex = endIndexParam.optional(args, context).filter(idx -> idx >= 0).orElse(length);

        return StringUtils.join(elements.subList(startIndex, endIndex), delimiter);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .pure(true)
                .returnType(String.class)
                .params(ImmutableList.of(elementsParam, delimiterParam, startIndexParam, endIndexParam))
                .description("Joins the elements of the provided array into a single String")
                .build();
    }
}
