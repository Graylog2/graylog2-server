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

import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.VarRefExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Emits expressions. Anything not emitted natively is evaluated by its AST node ("escape"), with identical semantics. */
final class ExpressionEmitter {
    static final Type LOGICAL = Type.getType(LogicalExpression.class);
    static final Method EVALUATE_UNSAFE = Method.getMethod(
            "Object evaluateUnsafe(org.graylog.plugins.pipelineprocessor.EvaluationContext)");
    static final Method EVALUATE_BOOL = Method.getMethod(
            "boolean evaluateBool(org.graylog.plugins.pipelineprocessor.EvaluationContext)");

    private final GeneratorAdapter g;
    private final RuleClassData data;
    private final Map<String, Integer> locals;
    private final Set<String> astVariables;
    private final List<String> escapes;
    private final FunctionCallEmitter functions;

    ExpressionEmitter(GeneratorAdapter g, RuleClassData data, Map<String, Integer> locals, Set<String> astVariables,
                      List<String> escapes, List<String> calls, FastPathResolver fastPaths) {
        this.g = g;
        this.data = data;
        this.locals = locals;
        this.astVariables = astVariables;
        this.escapes = escapes;
        this.functions = new FunctionCallEmitter(this, g, fastPaths, calls);
    }

    GeneratorAdapter generator() {
        return g;
    }

    /** Pushes the value of {@code expr} with the semantics of {@code expr.evaluateUnsafe(ctx)}. */
    void value(Expression expr) {
        switch (expr) {
            case VarRefExpression ref -> {
                final Integer slot = locals.get(ref.varName());
                if (slot == null) {
                    throw new UnsupportedConstructException("variable-outside-then");
                }
                g.loadLocal(slot);
            }
            case FunctionExpression call -> functions.call(call);
            default -> escapeValue(expr);
        }
    }

    /** Pushes 0/1 with the semantics of {@code ((LogicalExpression) expr).evaluateBool(ctx)}. */
    void bool(Expression expr) {
        escapeBool(expr);
    }

    void constant(Object value, Class<?> fieldType) {
        g.getStatic(RuleClassEmitter.OWNER, RuleClassData.fieldName(data.add(value, fieldType)), Type.getType(fieldType));
    }

    /** Remembers variables that an AST-evaluated subtree will read from the context. */
    void recordAstVariables(Expression expr) {
        if (expr instanceof VarRefExpression ref) {
            astVariables.add(ref.varName());
        }
        for (Expression child : expr.children()) {
            // a bare $message has a null child
            if (child != null) {
                recordAstVariables(child);
            }
        }
    }

    private void escapeValue(Expression expr) {
        recordAstVariables(expr);
        escapes.add(expr.getClass().getSimpleName());
        constant(expr, Expression.class);
        g.loadArg(0);
        g.invokeInterface(RuleClassEmitter.EXPRESSION, EVALUATE_UNSAFE);
    }

    private void escapeBool(Expression expr) {
        if (!(expr instanceof LogicalExpression)) {
            throw new UnsupportedConstructException("non-logical-condition-" + expr.getClass().getSimpleName());
        }
        recordAstVariables(expr);
        escapes.add(expr.getClass().getSimpleName());
        constant(expr, LogicalExpression.class);
        g.loadArg(0);
        g.invokeInterface(LOGICAL, EVALUATE_BOOL);
    }
}
