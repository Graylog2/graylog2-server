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

import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class LookupSetValue extends AbstractFunction<Object> {

    public static final String NAME = "lookup_set_value";

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, Object> keyParam;
    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Long, Long> ttlSecondsParam;

    @Inject
    public LookupSetValue(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .description("The existing lookup table to use to set the given value")
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keyParam = object("key")
                .description("The key to set in the lookup table")
                .build();
        valueParam = object("value")
                .description("The single value that should be set into the lookup table")
                .build();
        ttlSecondsParam = ParameterDescriptor.integer("ttl")
                .optional()
                .description("The time to live in seconds to assign to this entry")
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
        Object value = valueParam.required(args, context);
        if (value == null) {
            return null;
        }
        final Optional<Long> ttlSec = ttlSecondsParam.optional(args, context);
        if (ttlSec.isPresent()) {
            return table.setValueWithTtl(key, value, ttlSec.get()).singleValue();
        } else {
            return table.setValue(key, value).singleValue();
        }
    }

    @Override
    public FunctionDescriptor<Object> descriptor() {
        return FunctionDescriptor.builder()
                .name(NAME)
                .description("Set a single value in the named lookup table. Returns the new value on success, null on failure.")
                .params(lookupTableParam, keyParam, valueParam, ttlSecondsParam)
                .returnType(Object.class)
                .build();
    }
}
