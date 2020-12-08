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

import java.util.List;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class LookupAddStringList extends AbstractFunction<Object> {

    public static final String NAME = "lookup_add_string_list";

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, Object> keyParam;
    @SuppressWarnings("rawtypes")
    private final ParameterDescriptor<List, List> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> keepDuplicates;

    @Inject
    public LookupAddStringList(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .description("The existing lookup table to use to add the given list")
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keyParam = object("key")
                .description("The key to add in the lookup table")
                .build();
        valueParam = ParameterDescriptor.type("value", List.class)
                .description("The list value that should be added into the lookup table")
                .build();
        keepDuplicates = bool("keep_duplicates")
                .optional()
                .description("When adding values to an existing list, don't try to omit duplicates. Default is false")
                .build();
    }

    @Override
    public Object evaluate(FunctionArgs args, EvaluationContext context) {
        Object key = keyParam.required(args, context);
        if (key == null) {
            return null;
        }
        LookupTableService.Function table = lookupTableParam.required(args, context);
        if (table == null) {
            return null;
        }
        List<String> value = valueParam.required(args, context);
        if (value == null) {
            return null;
        }
        final boolean keepDupes = keepDuplicates.optional(args, context).orElse(false);

        return table.addStringList(key, value, keepDupes).stringListValue();
    }

    @Override
    public FunctionDescriptor<Object> descriptor() {
        return FunctionDescriptor.builder()
                .name(NAME)
                .description("Add a string list in the named lookup table. Returns the updated list on success, null on failure.")
                .params(lookupTableParam, keyParam, valueParam, keepDuplicates)
                .returnType(List.class)
                .build();
    }
}
