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
package org.graylog.plugins.pipelineprocessor.functions.lookup;

import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.common.reflect.TypeToken;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;

import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class LookupAll extends AbstractFunction<List<Object>> {

    public static final String NAME = "lookup_all";

    @SuppressWarnings("unchecked")
    private static final Class<List<Object>> LIST_RETURN_TYPE = (Class<List<Object>>) new TypeToken<List<Object>>() {
    }.getRawType();

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, List<Object>> keysParam;

    @Inject
    public LookupAll(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .description("The existing lookup table to use to lookup the given keys")
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keysParam = object("keys", LIST_RETURN_TYPE)
                .description("The keys to lookup in the table")
                .transform(this::transformToList)
                .build();
    }

    @Override
    public List<Object> evaluate(FunctionArgs args, EvaluationContext context) {
        final List<Object> keys = keysParam.required(args, context);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        LookupTableService.Function table = lookupTableParam.required(args, context);
        if (table == null) {
            return List.of();
        }
        final List<Object> results = new ArrayList<>();
        for (Object key : keys) {
            LookupResult result = table.lookup(key);
            if (result != null && !result.isEmpty()) {
                results.add(result.singleValue());
            }
        }
        return results;
    }

    @Override
    public FunctionDescriptor<List<Object>> descriptor() {
        return FunctionDescriptor.<List<Object>>builder()
                .name(NAME)
                .description("Looks up all provided values in the named lookup table, and returns all results as an array.")
                .params(lookupTableParam, keysParam)
                .returnType(LIST_RETURN_TYPE)
                .ruleBuilderEnabled()
                .ruleBuilderName("Lookup all values in lookup table")
                .ruleBuilderTitle("Lookup all values from '${keys}' in '${lookup_table}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.LOOKUP)
                .build();
    }

    private List<Object> transformToList(Object value) {
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).stream()
                    .map(LookupAll::convertValue)
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
}
