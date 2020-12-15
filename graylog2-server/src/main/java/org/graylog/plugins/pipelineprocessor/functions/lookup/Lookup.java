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
import com.google.inject.TypeLiteral;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;

import java.util.Collections;
import java.util.Map;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog2.plugin.lookup.LookupResult.SINGLE_VALUE_KEY;

public class Lookup extends AbstractFunction<Map<Object, Object>> {

    public static final String NAME = "lookup";

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, Object> keyParam;
    private final ParameterDescriptor<Object, Object> defaultParam;

    @Inject
    public Lookup(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .description("The existing lookup table to use to lookup the given key")
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keyParam = object("key")
                .description("The key to lookup in the table")
                .build();
        defaultParam = object("default")
                .description("The default multi value that should be used if there is no lookup result")
                .optional()
                .build();
    }

    @Override
    public Map<Object, Object> evaluate(FunctionArgs args, EvaluationContext context) {
        Object key = keyParam.required(args, context);
        if (key == null) {
            return Collections.singletonMap(SINGLE_VALUE_KEY, defaultParam.optional(args, context).orElse(null));
        }
        LookupTableService.Function table = lookupTableParam.required(args, context);
        if (table == null) {
            return Collections.singletonMap(SINGLE_VALUE_KEY, defaultParam.optional(args, context).orElse(null));
        }
        LookupResult result = table.lookup(key);
        if (result == null || result.isEmpty()) {
            return Collections.singletonMap(SINGLE_VALUE_KEY, defaultParam.optional(args, context).orElse(null));
        }
        return result.multiValue();
    }

    @Override
    public FunctionDescriptor<Map<Object, Object>> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.<Map<Object, Object>>builder()
                .name(NAME)
                .description("Looks up a multi value in the named lookup table.")
                .params(lookupTableParam, keyParam, defaultParam)
                .returnType((Class<? extends Map<Object, Object>>) new TypeLiteral<Map<Object, Object>>() {}.getRawType())
                .build();
    }
}
