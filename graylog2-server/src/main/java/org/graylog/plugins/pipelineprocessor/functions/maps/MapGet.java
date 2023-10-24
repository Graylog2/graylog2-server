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
package org.graylog.plugins.pipelineprocessor.functions.maps;

import com.google.common.base.Strings;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class MapGet extends AbstractFunction<Object> {
    public static final String NAME = "map_get";
    private static final String MAPARG = "map";
    private static final String KEYARG = "key";
    private final ParameterDescriptor<Map, Map> mapParam;
    private final ParameterDescriptor<String, String> keyParam;

    public MapGet() {
        mapParam = type(MAPARG, Map.class).description("A map").build();
        keyParam = string(KEYARG).ruleBuilderVariable().description("Get the value for this key in map").build();
    }

    @Override
    public Object evaluate(FunctionArgs args, EvaluationContext context) {

        final Map<String, Object> mapValue = mapParam.required(args, context);
        final String keyValue = keyParam.required(args, context);
        if (mapValue == null || Strings.isNullOrEmpty(keyValue)) {
            return null;
        }

        return mapValue.get(keyValue);
    }

    @Override
    public FunctionDescriptor<Object> descriptor() {
        return FunctionDescriptor.builder()
                .name(NAME)
                .returnType(Object.class)
                .params(of(mapParam, keyParam))
                .description("Get a value from a map")
                .ruleBuilderEnabled()
                .ruleBuilderName("Get value for key in map")
                .ruleBuilderTitle("Get value for '${key}' from map '${map}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.OTHER)
                .build();
    }
}
