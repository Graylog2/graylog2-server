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
package org.graylog.plugins.pipelineprocessor.functions.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

import static com.google.common.collect.ImmutableList.of;

public class JsonFlatten extends AbstractFunction<JsonNode> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonFlatten.class);
    public static final String NAME = "flatten_json";
    private static final String OPTION_JSON = "json";
    private static final String OPTION_FLATTEN = "flatten";
    private static final String OPTION_IGNORE = "ignore";

    public static final JsonUtils.ExtractFlags FLAGS_JSON = JsonUtils.ExtractFlags.builder()
            .flattenObjects(true)
            .escapeArrays(true)
            .deleteArrays(false)
            .build();
    public static final JsonUtils.ExtractFlags FLAGS_FLATTEN = JsonUtils.ExtractFlags.builder()
            .flattenObjects(true)
            .escapeArrays(false)
            .deleteArrays(false)
            .build();
    public static final JsonUtils.ExtractFlags FLAGS_IGNORE = JsonUtils.ExtractFlags.builder()
            .flattenObjects(true)
            .escapeArrays(false)
            .deleteArrays(true)
            .build();

    private final ObjectMapper objectMapper;
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> arrayHandlerParam;

    @Inject
    public JsonFlatten(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        valueParam = ParameterDescriptor.string("value").description("The string to parse as a JSON tree").build();
        arrayHandlerParam = ParameterDescriptor.string("array_handler").description("Determines how arrays are processed").build();
    }

    @Override
    public JsonNode evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final String arrayHandler = arrayHandlerParam.required(args, context);

        try {
            switch (arrayHandler) {
                case OPTION_IGNORE:
                    // ignore all top-level arrays
                    return JsonUtils.extractJson(value, objectMapper, FLAGS_IGNORE);
                case OPTION_JSON:
                    // return top-level arrays as valid JSON strings
                    return JsonUtils.extractJson(value, objectMapper, FLAGS_JSON);
                case OPTION_FLATTEN:
                    // explode all arrays and objects into top-level key/values
                    return JsonUtils.extractJson(value, objectMapper, FLAGS_FLATTEN);
                default:
                    LOG.warn("Unknown parameter array_handler: {}", arrayHandler);
            }
        } catch (IOException e) {
            LOG.warn("Unable to parse JSON", e);
        }
        return null;
    }

    @Override
    public FunctionDescriptor<JsonNode> descriptor() {
        return FunctionDescriptor.<JsonNode>builder()
                .name(NAME)
                .returnType(JsonNode.class)
                .params(of(
                        valueParam, arrayHandlerParam
                ))
                .description("Parses a string as a JSON tree, while flattening all containers to a single level")
                .build();
    }

}
