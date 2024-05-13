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

import jakarta.annotation.Nullable;
import org.antlr.v4.runtime.CommonToken;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ConstantExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineFunctionProxy implements ProxyObject {

    private final FunctionRegistry functionRegistry;
    private final EvaluationContext evaluationContext;

    public PipelineFunctionProxy(FunctionRegistry functionRegistry, EvaluationContext evaluationContext) {
        this.functionRegistry = functionRegistry;
        this.evaluationContext = evaluationContext;
    }

    @Override
    public java.util.function.Function<Map<String, Object>, ?> getMember(String key) {
        final Function<?> function = functionRegistry.resolve(key);
        return (Map<String, Object> args) -> function.evaluate(transformArgs(function, args), evaluationContext);
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

    private FunctionArgs transformArgs(Function<?> function, Map<String, Object> args) {
        final Map<String, Expression> expressionMap = function.descriptor().params().stream()
                .filter(descriptor -> args.containsKey(descriptor.name()))
                .collect(Collectors.toMap(ParameterDescriptor::name,
                        descriptor -> toExpression(args.get(descriptor.name()), descriptor)));

        return new FunctionArgs(function, expressionMap);
    }

    private ParameterExpression toExpression(Object value, ParameterDescriptor<?, ?> descriptor) {
        final Class<?> targetType = descriptor.type();
        if (value instanceof Number number) {
            if (targetType == Integer.class) {
                return new ParameterExpression(number.intValue(), targetType);
            }
            if (targetType == Long.class) {
                return new ParameterExpression(number.longValue(), targetType);
            }
            if (targetType == Float.class) {
                return new ParameterExpression(number.floatValue(), targetType);
            }
            if (targetType == Double.class) {
                return new ParameterExpression(number.doubleValue(), targetType);
            }
        }

        return new ParameterExpression(value, targetType);
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
