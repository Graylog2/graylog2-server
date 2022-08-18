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

import com.google.inject.Inject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class LookupStringListContains extends AbstractFunction<Boolean> {

    public static final String NAME = "lookup_string_list_contains";

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, Object> keyParam;
    private final ParameterDescriptor<Object, Object> valueParam;

    @Inject
    public LookupStringListContains(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .description("The existing lookup table to use to lookup the given key")
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keyParam = object("key")
                .description("The key to lookup in the table")
                .build();
        valueParam = object("value")
                .description("The value to lookup in the list referenced by the key")
                .build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        Object value = valueParam.required(args, context);
        if (value == null) {
            return false;
        }
        Object key = keyParam.required(args, context);
        if (key == null) {
            return false;
        }
        LookupTableService.Function table = lookupTableParam.required(args, context);
        if (table == null) {
            return false;
        }
        LookupResult result = table.lookup(key);
        if (result == null || result.isEmpty()) {
            return false;
        }
        return result.stringListValue().contains(value);
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .description("Looks up a value in the string list referenced by the key in the named lookup table.")
                .params(lookupTableParam, keyParam, valueParam)
                .returnType(Boolean.class)
                .build();
    }
}
