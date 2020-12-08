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
package org.graylog.plugins.pipelineprocessor.ast.expressions;

import com.google.common.base.Joiner;

import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.exceptions.FunctionEvaluationException;
import org.graylog.plugins.pipelineprocessor.ast.exceptions.LocationAwareEvalException;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;

import java.util.Map;
import java.util.stream.Collectors;

public class FunctionExpression extends BaseExpression {
    private final FunctionArgs args;
    private final Function<?> function;
    private final FunctionDescriptor descriptor;

    public FunctionExpression(Token start, FunctionArgs args) {
        super(start);
        this.args = args;
        this.function = args.getFunction();
        this.descriptor = this.function.descriptor();

        // precomputes all constant arguments to avoid dynamically recomputing trees on every invocation
        this.function.preprocessArgs(args);
    }

    public Function<?> getFunction() {
        return function;
    }

    public FunctionArgs getArgs() {
        return args;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        try {
            return descriptor.returnType().cast(function.evaluate(args, context));
        } catch (LocationAwareEvalException laee) {
            // the exception already has a location from the input source, simply propagate it.
            throw laee;
        } catch (Exception e) {
            // we need to wrap the original exception to retain the position in the tree where the exception originated
            throw new FunctionEvaluationException(this, e);
        }
    }

    @Override
    public Class getType() {
        return descriptor.returnType();
    }

    @Override
    public String toString() {
        String argsString = "";
        if (args != null) {
            argsString = Joiner.on(", ")
                    .withKeyValueSeparator(": ")
                    .join(args.getArgs().entrySet().stream()
                                  .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                                  .iterator());
        }
        return descriptor.name() + "(" + argsString + ")";
    }

    @Override
    public Iterable<Expression> children() {
        return args.getArgs().entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }
}
