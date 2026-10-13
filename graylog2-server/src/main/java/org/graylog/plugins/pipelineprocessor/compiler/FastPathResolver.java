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
package org.graylog.plugins.pipelineprocessor.compiler;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.PipelineFastPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

/** Finds a function's {@link PipelineFastPath} method and adapts it to an all-Object method handle. */
final class FastPathResolver {
    private static final Logger LOG = LoggerFactory.getLogger(FastPathResolver.class);

    /** {@code handle} has type {@code (Object × parameterCount [+ ctx]) -> Object} and is bound to the function. */
    record FastPath(MethodHandle handle, int parameterCount, boolean takesContext) {}

    private final Map<Function<?>, Optional<FastPath>> cache = Collections.synchronizedMap(new IdentityHashMap<>());

    Optional<FastPath> resolve(Function<?> function) {
        return cache.computeIfAbsent(function, fn -> {
            try {
                return Optional.ofNullable(validate(fn));
            } catch (IllegalStateException e) {
                LOG.warn("Ignoring fast path: {}", e.getMessage());
                return Optional.empty();
            }
        });
    }

    /** Returns the fast path, {@code null} if the function has none, or throws if annotated methods don't match. */
    @Nullable
    static FastPath validate(Function<?> function) {
        final List<Method> annotated = Arrays.stream(function.getClass().getMethods())
                .filter(m -> m.isAnnotationPresent(PipelineFastPath.class)).toList();
        if (annotated.isEmpty()) {
            return null;
        }
        final FunctionDescriptor<?> descriptor = function.descriptor();
        final List<ParameterDescriptor> params = descriptor.params();
        final List<Method> matching = annotated.stream().filter(m -> matches(m, params, descriptor.returnType())).toList();
        if (matching.size() != 1) {
            throw new IllegalStateException(f(
                    "function %s: expected exactly one @PipelineFastPath method matching %d parameter(s), found %d of %d annotated",
                    descriptor.name(), params.size(), matching.size(), annotated.size()));
        }
        final Method method = matching.get(0);
        try {
            final MethodHandle bound = MethodHandles.publicLookup().unreflect(method).bindTo(function);
            final MethodHandle generic = bound.asType(MethodType.genericMethodType(bound.type().parameterCount()));
            return new FastPath(generic, params.size(), method.getParameterCount() == params.size() + 1);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(f("function %s: fast path is not accessible", descriptor.name()), e);
        }
    }

    private static boolean matches(Method m, List<ParameterDescriptor> params, Class<?> returnType) {
        if (Modifier.isStatic(m.getModifiers())) {
            return false;
        }
        final Class<?>[] types = m.getParameterTypes();
        final boolean withContext = types.length == params.size() + 1 && types[types.length - 1] == EvaluationContext.class;
        if (types.length != params.size() && !withContext) {
            return false;
        }
        for (int i = 0; i < params.size(); i++) {
            if (types[i].isPrimitive() || !types[i].isAssignableFrom(params.get(i).transformedType())) {
                return false;
            }
        }
        return !m.getReturnType().isPrimitive() && returnType.isAssignableFrom(m.getReturnType());
    }
}
