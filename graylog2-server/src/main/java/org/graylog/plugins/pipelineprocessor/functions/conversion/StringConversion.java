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
package org.graylog.plugins.pipelineprocessor.functions.conversion;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.functions.ips.IpAddress;
import org.joda.time.DateTime;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class StringConversion extends AbstractFunction<String> {

    public static final String NAME = "to_string";

    // this is per-thread to save an expensive concurrent hashmap access
    private final ThreadLocal<LinkedHashMap<Class<?>, Class<?>>> declaringClassCache;
    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<String, String> defaultParam;

    public StringConversion() {
        declaringClassCache = new ThreadLocal<LinkedHashMap<Class<?>, Class<?>>>() {
            @Override
            protected LinkedHashMap<Class<?>, Class<?>> initialValue() {
                return new LinkedHashMap<Class<?>, Class<?>>() {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Class<?>, Class<?>> eldest) {
                        return size() > 1024;
                    }
                };
            }
        };
        valueParam = object("value").description("Value to convert").build();
        defaultParam = string("default").optional().description("Used when 'value' is null, defaults to \"\"").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = valueParam.required(args, context);
        if (evaluated == null) {
            return defaultParam.optional(args, context).orElse("");
        }
        // fast path for the most common targets
        if (evaluated instanceof String
                || evaluated instanceof Number
                || evaluated instanceof Boolean
                || evaluated instanceof DateTime
                || evaluated instanceof IpAddress) {
            return evaluated.toString();
        } else {
            //noinspection Duplicates
            try {
                // slow path, we aren't sure that the object's class actually overrides toString() so we'll look it up.
                final Class<?> klass = evaluated.getClass();
                final LinkedHashMap<Class<?>, Class<?>> classCache = declaringClassCache.get();

                Class<?> declaringClass = classCache.get(klass);
                if (declaringClass == null) {
                    declaringClass = klass.getMethod("toString").getDeclaringClass();
                    classCache.put(klass, declaringClass);
                }
                if ((declaringClass != Object.class)) {
                    return evaluated.toString();
                } else {
                    return defaultParam.optional(args, context).orElse("");
                }
            } catch (NoSuchMethodException ignored) {
                // should never happen because toString is always there
                return defaultParam.optional(args, context).orElse("");
            }
        }
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(of(
                        valueParam,
                        defaultParam
                ))
                .description("Converts a value to its string representation")
                .build();
    }
}
