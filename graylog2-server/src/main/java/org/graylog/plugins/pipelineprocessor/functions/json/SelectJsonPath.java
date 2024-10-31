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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.TypeLiteral;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Collectors.toMap;

public class SelectJsonPath extends AbstractFunction<Map<String, Object>> {

    public static final String NAME = "select_jsonpath";

    private final ObjectMapper objectMapper;
    private final Configuration configuration;
    private final ParameterDescriptor<Object, Object> jsonParam;
    private final ParameterDescriptor<Map<String, String>, Map<String, JsonPath>> pathsParam;
    private final ParameterDescriptor<Boolean, Boolean> excludeEmptyArraysParam;


    @Inject
    public SelectJsonPath(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        configuration = Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .jsonProvider(new JacksonJsonNodeJsonProvider(objectMapper))
                .build();

        jsonParam = ParameterDescriptor.type("json", Object.class).description("A parsed JSON tree or String representation of a JSON tree").build();
        // sigh generics and type erasure
        //noinspection unchecked
        pathsParam = ParameterDescriptor.type("paths",
                        (Class<Map<String, String>>) new TypeLiteral<Map<String, String>>() {}.getRawType(),
                        (Class<Map<String, JsonPath>>) new TypeLiteral<Map<String, JsonPath>>() {}.getRawType())
                .transform(inputMap -> inputMap
                        .entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, e -> JsonPath.compile(e.getValue()))))
                .description("A map of names to a JsonPath expression, see http://jsonpath.com")
                .build();
        excludeEmptyArraysParam = ParameterDescriptor.bool("exclude_empty_arrays").optional().description("Exclude any empty arrays from results map").build();
    }

    @Override
    public Map<String, Object> evaluate(FunctionArgs args, EvaluationContext context) {
        final Object jsonObj = jsonParam.required(args, context);
        final boolean excludeEmptyArrays = excludeEmptyArraysParam.optional(args, context).orElse(false);

        JsonNode json = null;
        if (jsonObj instanceof JsonNode jsonNode) {
            json = jsonNode;
        } else if (jsonObj instanceof String jsonString) {
            try {
                json = objectMapper.readTree(jsonString);
            } catch (JsonProcessingException e) {
                log.warn(context.pipelineErrorMessage("Unable to parse JSON"), e);
            }
        } else {
            throw new IllegalArgumentException(context.pipelineErrorMessage(
                    "`json` parameter must be a parsed JSON tree or String representation of a JSON tree"));
        }

        final Map<String, JsonPath> paths = pathsParam.required(args, context);
        if (json == null || paths == null) {
            return Collections.emptyMap();
        }
        // a plain Stream.collect(toMap(...)) will fail on null values, because of the HashMap#merge method in its implementation
        // since json nodes at certain paths might be missing, the value could be null, so we use HashMap#put directly
        final Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonPath> entry : paths.entrySet()) {
            final Object obj = unwrapJsonNode(entry.getValue().read(json, configuration));
            if (obj instanceof List list && list.isEmpty() && excludeEmptyArrays) {
                continue;
            }
            map.put(entry.getKey(), obj);
        }
        return map;
    }

    @Nullable
    private Object unwrapJsonNode(Object value) {
        if (!(value instanceof JsonNode)) {
            return value;
        }
        JsonNode read = ((JsonNode) value);
        switch (read.getNodeType()) {
            case ARRAY:
                return ImmutableList.copyOf(read.elements());
            case BINARY:
                try {
                    return read.binaryValue();
                } catch (IOException e) {
                    return null;
                }
            case BOOLEAN:
                return read.booleanValue();
            case MISSING, NULL:
                return null;
            case NUMBER:
                return read.numberValue();
            case OBJECT, POJO:
                return read;
            case STRING:
                return read.textValue();
        }
        return read;
    }

    @Override
    public FunctionDescriptor<Map<String, Object>> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.<Map<String, Object>>builder()
                .name(NAME)
                .returnType((Class<? extends Map<String, Object>>) new TypeLiteral<Map<String, Object>>() {}.getRawType())
                .params(of(
                        jsonParam,
                        pathsParam,
                        excludeEmptyArraysParam
                ))
                .description("Selects a map of fields containing the result of their JsonPath expressions")
                .build();
    }

}
