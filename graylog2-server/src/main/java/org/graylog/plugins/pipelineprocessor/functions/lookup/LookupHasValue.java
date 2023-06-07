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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;

import javax.inject.Inject;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class LookupHasValue extends AbstractFunction<Object> {

    public static final String NAME = "lookup_has_value";

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, Object> keyParam;

    @Inject
    public LookupHasValue(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .description("The existing lookup table to use to lookup the given key")
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keyParam = object("key")
                .description("The key to lookup in the table")
                .build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        Object key = keyParam.required(args, context);
        if (key == null) {
            return false;
        }
        LookupTableService.Function table = lookupTableParam.required(args, context);
        if (table == null) {
            return false;
        }
        LookupResult result = table.lookup(key);
        return (result != null && !result.isEmpty());
    }

    @Override
    public FunctionDescriptor<Object> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.builder()
                .name(NAME)
                .description("Checks if lookup table contains a given key.")
                .params(lookupTableParam, keyParam)
                .returnType(Boolean.class)
                .build();
    }
}
