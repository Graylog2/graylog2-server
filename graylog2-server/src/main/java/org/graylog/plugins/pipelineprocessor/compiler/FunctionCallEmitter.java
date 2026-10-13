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

import org.graylog.plugins.pipelineprocessor.ast.exceptions.FunctionEvaluationException;
import org.graylog.plugins.pipelineprocessor.ast.exceptions.LocationAwareEvalException;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Emits function calls: through a function's {@code @PipelineFastPath} method when eligible, else via its AST node. */
final class FunctionCallEmitter {
    static final Type FUNCTION_EXPRESSION = Type.getType(FunctionExpression.class);
    static final Method EVALUATE_UNSAFE = Method.getMethod(
            "Object evaluateUnsafe(org.graylog.plugins.pipelineprocessor.EvaluationContext)");
    static final Type FUNCTION_EVALUATION_EXCEPTION = Type.getType(FunctionEvaluationException.class);
    static final Type LOCATION_AWARE = Type.getType(LocationAwareEvalException.class);
    static final Type METHOD_HANDLE = Type.getType(MethodHandle.class);
    static final Type PARAMETER_DESCRIPTOR = Type.getType(ParameterDescriptor.class);
    static final Type CLASS = Type.getType(Class.class);
    static final Method WARN_IF_DEPRECATED = Method.getMethod(
            "void warnIfDeprecated(org.graylog.plugins.pipelineprocessor.EvaluationContext)");
    static final Method CONVERT = Method.getMethod("Object convert(Object)");
    static final Method CAST = Method.getMethod("Object cast(Object)");
    static final Method FEE_INIT = Method.getMethod(
            "void <init>(org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression, Exception)");

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
        final Optional<FastPathResolver.FastPath> fastPath = fastPaths.resolve(f.getFunction());
        if (fastPath.isPresent() && eligible(f)) {
            fast(f, fastPath.get());
        } else {
            legacy(f);
        }
    }

    /** Only the first argument may be computed at runtime; later ones must be absent or precomputed constants. */
    static boolean eligible(FunctionExpression f) {
        final List<ParameterDescriptor> params = f.getFunction().descriptor().params();
        for (int i = 1; i < params.size(); i++) {
            final String name = params.get(i).name();
            if (f.getArgs().getPreComputedValue(name) == null && f.getArgs().expression(name) != null) {
                return false;
            }
        }
        return true;
    }

    /** Same steps as FunctionExpression.evaluateUnsafe + ParameterDescriptor.required, without FunctionArgs. */
    private void fast(FunctionExpression f, FastPathResolver.FastPath fastPath) {
        calls.add("fastPath." + f.getFunction().descriptor().name());
        final Label tryStart = g.mark();
        exprs.constant(f, FunctionExpression.class);
        g.loadArg(0);
        g.invokeVirtual(FUNCTION_EXPRESSION, WARN_IF_DEPRECATED);
        exprs.constant(fastPath.handle(), MethodHandle.class);
        for (ParameterDescriptor param : f.getFunction().descriptor().params()) {
            argument(f, param);
        }
        if (fastPath.takesContext()) {
            g.loadArg(0);
        }
        final int arity = fastPath.parameterCount() + (fastPath.takesContext() ? 1 : 0);
        final Type[] objects = new Type[arity];
        Arrays.fill(objects, RuleClassEmitter.OBJECT);
        g.invokeVirtual(METHOD_HANDLE, new Method("invokeExact", RuleClassEmitter.OBJECT, objects));
        exprs.constant(f.getFunction().descriptor().returnType(), Class.class);
        g.swap();
        g.invokeVirtual(CLASS, CAST);
        final Label tryEnd = g.mark();
        final Label done = g.newLabel();
        g.goTo(done);
        // the exception already has a location from the input source, simply propagate it
        g.catchException(tryStart, tryEnd, LOCATION_AWARE);
        g.throwException();
        // wrap any other exception to retain the position in the tree where it originated
        g.catchException(tryStart, tryEnd, RuleClassEmitter.EXCEPTION);
        final int exception = g.newLocal(RuleClassEmitter.EXCEPTION);
        g.storeLocal(exception);
        g.newInstance(FUNCTION_EVALUATION_EXCEPTION);
        g.dup();
        exprs.constant(f, FunctionExpression.class);
        g.loadLocal(exception);
        g.invokeConstructor(FUNCTION_EVALUATION_EXCEPTION, FEE_INIT);
        g.throwException();
        g.mark(done);
    }

    private void argument(FunctionExpression f, ParameterDescriptor param) {
        final Object precomputed = f.getArgs().getPreComputedValue(param.name());
        if (precomputed != null) {
            exprs.constant(param.transformedType().cast(precomputed), Object.class);
            return;
        }
        final Expression expr = f.getArgs().expression(param.name());
        if (expr == null) {
            g.visitInsn(Opcodes.ACONST_NULL);
            return;
        }
        exprs.constant(param, ParameterDescriptor.class);
        exprs.value(expr);
        g.invokeVirtual(PARAMETER_DESCRIPTOR, CONVERT);
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
