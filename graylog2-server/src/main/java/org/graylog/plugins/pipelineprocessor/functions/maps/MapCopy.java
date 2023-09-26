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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class MapCopy extends AbstractFunction<Map> {
    public static final String NAME = "map_copy";
    private static final String MAPARG = "map";
    private final ParameterDescriptor<Map, Map> mapParam;

    public MapCopy() {
        mapParam = type(MAPARG, Map.class).ruleBuilderVariable().description("A map").build();
    }

    @Override
    public Map evaluate(FunctionArgs args, EvaluationContext context) {

        final Map<String, Object> mapValue = mapParam.required(args, context);
        if (mapValue == null) {
            return Collections.emptyMap();
        }

        return new HashMap<>(mapValue);
    }

    @Override
    public FunctionDescriptor<Map> descriptor() {
        return FunctionDescriptor.<Map>builder()
                .name(NAME)
                .returnType(Map.class)
                .params(of(mapParam))
                .description("Copy a map to a new map")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.OTHER)
                .build();
    }
}
