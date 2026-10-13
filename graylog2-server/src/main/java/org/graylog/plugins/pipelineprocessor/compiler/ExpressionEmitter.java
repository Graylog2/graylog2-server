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
import org.graylog.plugins.pipelineprocessor.ast.expressions.AdditionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.AndExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanValuedFunctionWrapper;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ComparisonExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ConstantExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.DoubleExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.EqualityExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FieldRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LongExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MessageRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MultiplicationExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.NotExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.OrExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.PipelineOps;
import org.graylog.plugins.pipelineprocessor.ast.expressions.SignedExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.StringExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.VarRefExpression;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
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
    static final Type PIPELINE_OPS = Type.getType(PipelineOps.class);
    static final Type MESSAGE = Type.getType(Message.class);
    static final Method CURRENT_MESSAGE = Method.getMethod("org.graylog2.plugin.Message currentMessage()");
    static final Method GET_FIELD = Method.getMethod("Object getField(String)");
    static final Method MESSAGE_FIELD = Method.getMethod(
            "Object messageField(org.graylog.plugins.pipelineprocessor.EvaluationContext, Object)");
    static final Method TO_BOOL = Method.getMethod("boolean toBool(Object)");
    static final Method COMPARE = Method.getMethod("boolean compare(String, Object, Object)");
    static final Method EQUALITY = Method.getMethod(
            "boolean equality(org.graylog.plugins.pipelineprocessor.ast.expressions.EqualityExpression, "
                    + "org.graylog.plugins.pipelineprocessor.EvaluationContext, Object, Object)");
    static final Method ADD = Method.getMethod(
            "Object add(org.graylog.plugins.pipelineprocessor.ast.expressions.AdditionExpression, Object, Object)");
    static final Method MULTIPLY = Method.getMethod(
            "Object multiply(org.graylog.plugins.pipelineprocessor.ast.expressions.MultiplicationExpression, "
                    + "org.graylog.plugins.pipelineprocessor.EvaluationContext, Object, Object)");
    static final Method SIGNED = Method.getMethod(
            "Object signed(org.graylog.plugins.pipelineprocessor.ast.expressions.SignedExpression, "
                    + "org.graylog.plugins.pipelineprocessor.EvaluationContext, Object)");

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
        if (fold(expr)) {
            return;
        }
        switch (expr) {
            case StringExpression s -> g.push((String) s.evaluateUnsafe(EvaluationContext.emptyContext()));
            case LongExpression l -> constant(l.evaluateUnsafe(EvaluationContext.emptyContext()), Object.class);
            case DoubleExpression d -> constant(d.evaluateUnsafe(EvaluationContext.emptyContext()), Object.class);
            case FieldRefExpression f -> g.push(f.fieldName());
            case MessageRefExpression m -> messageRef(m);
            case VarRefExpression ref -> {
                final Integer slot = locals.get(ref.varName());
                if (slot == null) {
                    throw new UnsupportedConstructException("variable-outside-then");
                }
                g.loadLocal(slot);
            }
            case FunctionExpression call -> functions.call(call);
            case BooleanExpression b -> {
                bool(b);
                g.valueOf(Type.BOOLEAN_TYPE);
            }
            case AndExpression a -> {
                bool(a);
                g.valueOf(Type.BOOLEAN_TYPE);
            }
            case OrExpression o -> {
                bool(o);
                g.valueOf(Type.BOOLEAN_TYPE);
            }
            case NotExpression n -> {
                bool(n);
                g.valueOf(Type.BOOLEAN_TYPE);
            }
            case BooleanValuedFunctionWrapper w -> {
                bool(w);
                g.valueOf(Type.BOOLEAN_TYPE);
            }
            // operands are pushed left first, then right: the interpreter's evaluation order; pushing the node, the
            // context or the operator has no side effects
            case AdditionExpression a -> {
                constant(a, AdditionExpression.class);
                value(a.left());
                value(a.right());
                g.invokeStatic(PIPELINE_OPS, ADD);
            }
            case MultiplicationExpression m -> {
                constant(m, MultiplicationExpression.class);
                g.loadArg(0);
                value(m.left());
                value(m.right());
                g.invokeStatic(PIPELINE_OPS, MULTIPLY);
            }
            case SignedExpression s -> {
                constant(s, SignedExpression.class);
                g.loadArg(0);
                value(s.right());
                g.invokeStatic(PIPELINE_OPS, SIGNED);
            }
            case ComparisonExpression c -> {
                bool(c);
                g.valueOf(Type.BOOLEAN_TYPE);
            }
            case EqualityExpression e -> {
                bool(e);
                g.valueOf(Type.BOOLEAN_TYPE);
            }
            default -> escapeValue(expr);
        }
    }

    /** Pushes 0/1 with the semantics of {@code ((LogicalExpression) expr).evaluateBool(ctx)}. */
    void bool(Expression expr) {
        if (foldBool(expr)) {
            return;
        }
        switch (expr) {
            case BooleanExpression b -> g.push(b.evaluateBool(EvaluationContext.emptyContext()));
            case AndExpression a -> {
                final Label isFalse = g.newLabel();
                final Label end = g.newLabel();
                bool(a.left());
                g.ifZCmp(GeneratorAdapter.EQ, isFalse);
                bool(a.right());
                g.goTo(end);
                g.mark(isFalse);
                g.push(false);
                g.mark(end);
            }
            case OrExpression o -> {
                final Label isTrue = g.newLabel();
                final Label end = g.newLabel();
                bool(o.left());
                g.ifZCmp(GeneratorAdapter.NE, isTrue);
                bool(o.right());
                g.goTo(end);
                g.mark(isTrue);
                g.push(true);
                g.mark(end);
            }
            case NotExpression n -> {
                bool(n.right());
                g.push(true);
                g.math(GeneratorAdapter.XOR, Type.INT_TYPE);
            }
            case BooleanValuedFunctionWrapper w -> {
                value(w.expression());
                g.invokeStatic(PIPELINE_OPS, TO_BOOL);
            }
            case ComparisonExpression c -> {
                g.push(c.getOperator());
                value(c.left());
                value(c.right());
                g.invokeStatic(PIPELINE_OPS, COMPARE);
            }
            case EqualityExpression e -> {
                constant(e, EqualityExpression.class);
                g.loadArg(0);
                value(e.left());
                value(e.right());
                g.invokeStatic(PIPELINE_OPS, EQUALITY);
            }
            default -> escapeBool(expr);
        }
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

    @SuppressWarnings("ReferenceEquality") // identity is the "no field" marker returned by getFieldExpr()
    private void messageRef(MessageRefExpression m) {
        final Expression fieldExpr = m.getFieldExpr();
        g.loadArg(0);
        if (fieldExpr == m) { // `$message` itself: getFieldExpr() returns the node when it has no field
            g.invokeVirtual(RuleClassEmitter.EVAL_CTX, CURRENT_MESSAGE);
        } else if (fieldExpr instanceof FieldRefExpression f) {
            g.invokeVirtual(RuleClassEmitter.EVAL_CTX, CURRENT_MESSAGE);
            g.push(f.fieldName());
            g.invokeVirtual(MESSAGE, GET_FIELD);
        } else {
            value(fieldExpr);
            g.invokeStatic(PIPELINE_OPS, MESSAGE_FIELD);
        }
    }

    /** Folds a constant subtree to its value, but only for immutable results and only if evaluating it succeeds. */
    private boolean fold(Expression expr) {
        if (!expr.isConstant() || expr instanceof ConstantExpression || containsVarRef(expr)) {
            return false;
        }
        final Object folded;
        try {
            folded = expr.evaluateUnsafe(EvaluationContext.emptyContext());
        } catch (Exception e) {
            return false; // keep the runtime evaluation so the error happens at the same place
        }
        if (folded == null) {
            g.visitInsn(Opcodes.ACONST_NULL);
            return true;
        }
        if (folded instanceof String || folded instanceof Long || folded instanceof Double || folded instanceof Boolean
                || folded instanceof DateTime || folded instanceof Period || folded instanceof Duration) {
            constant(folded, Object.class);
            return true;
        }
        return false;
    }

    private boolean foldBool(Expression expr) {
        if (!(expr instanceof LogicalExpression logical) || !expr.isConstant() || expr instanceof BooleanExpression
                || containsVarRef(expr)) {
            return false;
        }
        try {
            g.push(logical.evaluateBool(EvaluationContext.emptyContext()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean containsVarRef(Expression expr) {
        if (expr instanceof VarRefExpression) {
            return true;
        }
        for (Expression child : expr.children()) {
            // a bare $message has a null child
            if (child != null && containsVarRef(child)) {
                return true;
            }
        }
        return false;
    }
}
