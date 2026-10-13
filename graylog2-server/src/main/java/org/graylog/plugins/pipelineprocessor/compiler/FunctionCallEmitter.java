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

import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;

/** Emits function calls. Every call currently takes the legacy path through its AST node. */
final class FunctionCallEmitter {
    static final Type FUNCTION_EXPRESSION = Type.getType(FunctionExpression.class);
    static final Method EVALUATE_UNSAFE = Method.getMethod(
            "Object evaluateUnsafe(org.graylog.plugins.pipelineprocessor.EvaluationContext)");

    private final ExpressionEmitter exprs;
    private final GeneratorAdapter g;
    private final FastPathResolver fastPaths;
    private final List<String> calls;

    FunctionCallEmitter(ExpressionEmitter exprs, GeneratorAdapter g, FastPathResolver fastPaths, List<String> calls) {
        this.exprs = exprs;
        this.g = g;
        this.fastPaths = fastPaths;
        this.calls = calls;
    }

    void call(FunctionExpression f) {
        legacy(f);
    }

    /** {@code f.evaluateUnsafe(ctx)} on a constant node: the function evaluates its arguments through the AST. */
    private void legacy(FunctionExpression f) {
        exprs.recordAstVariables(f);
        calls.add("legacy." + f.getFunction().descriptor().name());
        exprs.constant(f, FunctionExpression.class);
        g.loadArg(0);
        g.invokeVirtual(FUNCTION_EXPRESSION, EVALUATE_UNSAFE);
    }
}
