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
package org.graylog.plugins.pipelineprocessor.ast.functions;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.exceptions.PrecomputeFailure;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public interface Function<T> {

    Logger log = LoggerFactory.getLogger(Function.class);

    Function ERROR_FUNCTION = new AbstractFunction<Void>() {
        @Override
        public Void evaluate(FunctionArgs args, EvaluationContext context) {
            return null;
        }

        @Override
        public void preprocessArgs(FunctionArgs args) {
            // intentionally left blank
        }

        @Override
        public FunctionDescriptor<Void> descriptor() {
            return FunctionDescriptor.<Void>builder()
                    .name("__unresolved_function")
                    .returnType(Void.class)
                    .params(ImmutableList.of())
                    .build();
        }
    };

    default void preprocessArgs(FunctionArgs args) {
        for (Map.Entry<String, Expression> e : args.getConstantArgs().entrySet()) {
            final String name = e.getKey();
            try {
                final Object value = preComputeConstantArgument(args, name, e.getValue());
                if (value != null) {
                    //noinspection unchecked
                    final ParameterDescriptor<Object, Object> param = (ParameterDescriptor<Object, Object>) args.param(name);
                    if (param == null) {
                        throw new IllegalStateException("Unknown parameter " + name + "! Cannot continue.");
                    }
                    args.setPreComputedValue(name, param.transform().apply(value));
                }
            } catch (Exception exception) {
                log.debug("Unable to precompute argument value for " + name, exception);
                throw new PrecomputeFailure(name, exception);
            }
        }

    }

    /**
     * Implementations should provide a non-null value for each argument they wish to pre-compute.
     * <br>
     * Examples include compile a Pattern from a regex string, which will never change during the lifetime of the function.
     * If any part of the expression tree depends on external values this method will not be called, e.g. if the regex depends on a message field.
     * @param args the function args for this functions, usually you don't need this
     * @param name the name of the argument to potentially precompute
     * @param arg the expression tree for the argument
     * @return the precomputed value for the argument or <code>null</code> if the value should be dynamically calculated for each invocation
     */
    Object preComputeConstantArgument(FunctionArgs args, String name, Expression arg);

    T evaluate(FunctionArgs args, EvaluationContext context);

    FunctionDescriptor<T> descriptor();

}
