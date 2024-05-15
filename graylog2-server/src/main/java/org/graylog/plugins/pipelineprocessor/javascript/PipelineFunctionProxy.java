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
package org.graylog.plugins.pipelineprocessor.javascript;

import com.google.common.primitives.Ints;
import jakarta.annotation.Nullable;
import org.antlr.v4.runtime.CommonToken;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ConstantExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

public class PipelineFunctionProxy implements ProxyObject {

    private final FunctionRegistry functionRegistry;
    private final EvaluationContext evaluationContext;

    public PipelineFunctionProxy(FunctionRegistry functionRegistry, EvaluationContext evaluationContext) {
        this.functionRegistry = functionRegistry;
        this.evaluationContext = evaluationContext;
    }

    @Override
    public ProxyExecutable getMember(String key) {
        final Function<?> function = functionRegistry.resolve(key);
        return arguments -> function.evaluate(transformArgs(function, arguments), evaluationContext);
    }

    @Override
    public List<String> getMemberKeys() {
        return functionRegistry.all().stream().map(f -> f.descriptor().name()).toList();
    }

    @Override
    public boolean hasMember(String key) {
        return functionRegistry.resolve(key) != null;
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Access to pipeline functions is read-only.");
    }

    private FunctionArgs transformArgs(Function<?> function, Value... args) {
        final var functionName = function.descriptor().name();
        final var params = function.descriptor().params();

        if (args.length != params.size()) {
            throw new IllegalArgumentException(
                    f("Number of arguments for function %s does not match. Expected %d but got %d.",
                            functionName,
                            params.size(), args.length));
        }

        final Map<String, Expression> expressionMap = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            final Value arg = args[i];
            final ParameterDescriptor<?, ?> paramDescriptor = params.get(i);
            expressionMap.put(
                    paramDescriptor.name(),
                    new ParameterExpression(convert(arg, paramDescriptor.type()), paramDescriptor.type()));
        }

        return new FunctionArgs(function, expressionMap);
    }

    private <T> T convert(Value value, Class<T> targetType) {
        if (value.isHostObject()) {
            return value.asHostObject();
        }

        if (value.hasArrayElements() && targetType.isAssignableFrom(List.class)) {
            final List<Object> list = new ArrayList<>(Ints.saturatedCast(value.getArraySize()));
            for (int i = 0; i < value.getArraySize(); i++) {
                list.add(convert(value.getArrayElement(i), Object.class));
            }
            return targetType.cast(list);
        }

        if (value.hasMembers() && targetType.isAssignableFrom(Map.class)) {
            final Map<?, ?> map = value.getMemberKeys().stream()
                    .collect(Collectors.toMap(k -> k, k -> convert(value.getMember(k), Object.class)));
            return targetType.cast(map);
        }

        return value.as(targetType);
    }

    private static class ParameterExpression extends ConstantExpression {
        private final Object value;

        public ParameterExpression(Object value, Class<?> type) {
            super(new CommonToken(0), type);
            this.value = value;
        }

        @Nullable
        @Override
        public Object evaluateUnsafe(EvaluationContext context) {
            return value;
        }
    }

}
