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
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.statements.FunctionStatement;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;
import org.graylog.plugins.pipelineprocessor.ast.statements.VarAssignStatement;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.V21;

/** Emits the class for one rule: {@code when}, {@code then}, constants, and the class-data initializer. */
final class RuleClassEmitter {
    static final String CLASS_NAME = "org/graylog/plugins/pipelineprocessor/compiler/GeneratedRule";
    static final Type OWNER = Type.getObjectType(CLASS_NAME);
    static final Type OBJECT = Type.getType(Object.class);
    static final Type EXCEPTION = Type.getType(Exception.class);
    static final Type EVAL_CTX = Type.getType(EvaluationContext.class);
    static final Type EXPRESSION = Type.getType(Expression.class);
    private static final Type LIST = Type.getType(List.class);
    private static final Type METHOD_HANDLES = Type.getType(MethodHandles.class);
    private static final Method LOOKUP = Method.getMethod("java.lang.invoke.MethodHandles$Lookup lookup()");
    private static final Method CLASS_DATA = Method.getMethod("Object classData(java.lang.invoke.MethodHandles$Lookup, String, Class)");
    private static final Method LIST_GET = Method.getMethod("Object get(int)");
    static final Method HAS_EVALUATION_ERRORS = Method.getMethod("boolean hasEvaluationErrors()");
    static final Method ON_EVALUATION_EXCEPTION = Method.getMethod(
            "void onEvaluationException(Exception, org.graylog.plugins.pipelineprocessor.ast.expressions.Expression)");
    static final Method DEFINE = Method.getMethod("void define(String, Class, Object)");

    // only carries the emission result to the loader, it is never compared or hashed
    @SuppressWarnings("ArrayRecordComponent")
    record Emitted(byte[] bytes, List<Object> classData, List<String> escapes, List<String> calls) {}

    private final Rule rule;
    private final FastPathResolver fastPaths;

    RuleClassEmitter(Rule rule, FastPathResolver fastPaths) {
        this.rule = rule;
        this.fastPaths = fastPaths;
    }

    Emitted emit() {
        // Dry run: find variables read by AST-evaluated subtrees; those must also be defined in the context.
        final Set<String> contextVariables = new HashSet<>();
        emitClass(Set.of(), contextVariables);
        return emitClass(Set.copyOf(contextVariables), new HashSet<>());
    }

    private Emitted emitClass(Set<String> contextVariables, Set<String> astVariableSink) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected ClassLoader getClassLoader() {
                return RuleClassEmitter.class.getClassLoader();
            }
        };
        cw.visit(V21, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, CLASS_NAME, null, "java/lang/Object",
                new String[]{Type.getInternalName(CompiledRule.class)});
        cw.visitSource(rule.name(), null);
        final RuleClassData data = new RuleClassData();
        final List<String> escapes = new ArrayList<>();
        final List<String> calls = new ArrayList<>();

        final GeneratorAdapter init = new GeneratorAdapter(ACC_PUBLIC, Method.getMethod("void <init>()"), null, null, cw);
        init.visitCode();
        init.loadThis();
        init.invokeConstructor(OBJECT, Method.getMethod("void <init>()"));
        init.returnValue();
        init.endMethod();

        final GeneratorAdapter when = method(cw, "boolean when(org.graylog.plugins.pipelineprocessor.EvaluationContext)");
        new ExpressionEmitter(when, data, Map.of(), astVariableSink, escapes, calls, fastPaths).bool(rule.when());
        when.returnValue();
        when.endMethod();

        emitThen(cw, data, contextVariables, astVariableSink, escapes, calls);
        emitConstants(cw, data);
        cw.visitEnd();
        return new Emitted(cw.toByteArray(), data.values(), escapes, calls);
    }

    private void emitThen(ClassWriter cw, RuleClassData data, Set<String> contextVariables, Set<String> astVariableSink,
                          List<String> escapes, List<String> calls) {
        final GeneratorAdapter g = method(cw, "boolean then(org.graylog.plugins.pipelineprocessor.EvaluationContext)");
        final Map<String, Integer> locals = new HashMap<>();
        for (Statement statement : rule.then()) {
            if (statement instanceof VarAssignStatement assign && !locals.containsKey(assign.getName())) {
                final int slot = g.newLocal(OBJECT);
                g.visitInsn(ACONST_NULL);
                g.storeLocal(slot);
                locals.put(assign.getName(), slot);
            }
        }
        final ExpressionEmitter exprs = new ExpressionEmitter(g, data, locals, astVariableSink, escapes, calls, fastPaths);
        for (Statement statement : rule.then()) {
            switch (statement) {
                case FunctionStatement fs -> statement(g, exprs, fs.getFunctionExpression(), -1);
                case VarAssignStatement assign -> {
                    final int slot = locals.get(assign.getName());
                    statement(g, exprs, assign.getValueExpression(), slot);
                    if (contextVariables.contains(assign.getName())) {
                        // an AST-evaluated subtree reads this variable from the context, as the interpreter would
                        g.loadArg(0);
                        g.push(assign.getName());
                        exprs.constant(assign.getValueExpression().getType(), Class.class);
                        g.loadLocal(slot);
                        g.invokeVirtual(EVAL_CTX, DEFINE);
                    }
                }
                default -> throw new UnsupportedConstructException("statement-" + statement.getClass().getSimpleName());
            }
            // the interpreter stops a rule after the first statement that leaves an evaluation error behind
            g.loadArg(0);
            g.invokeVirtual(EVAL_CTX, HAS_EVALUATION_ERRORS);
            final Label ok = g.newLabel();
            g.ifZCmp(GeneratorAdapter.EQ, ok);
            g.push(false);
            g.returnValue();
            g.mark(ok);
        }
        g.push(true);
        g.returnValue();
        g.endMethod();
    }

    /** Same as {@code expr.evaluate(ctx)} in a statement: exceptions are recorded and the value becomes null. */
    private static void statement(GeneratorAdapter g, ExpressionEmitter exprs, Expression expr, int slot) {
        final Label start = g.mark();
        if (expr.getStartToken() != null) {
            g.visitLineNumber(expr.getStartToken().getLine(), start);
        }
        exprs.value(expr);
        if (slot < 0) {
            g.pop();
        } else {
            g.storeLocal(slot);
        }
        final Label end = g.mark();
        final Label after = g.newLabel();
        g.goTo(after);
        g.catchException(start, end, EXCEPTION);
        final int exception = g.newLocal(EXCEPTION);
        g.storeLocal(exception);
        g.loadArg(0);
        g.loadLocal(exception);
        exprs.constant(expr, Expression.class);
        g.invokeVirtual(EVAL_CTX, ON_EVALUATION_EXCEPTION);
        if (slot >= 0) {
            g.visitInsn(ACONST_NULL);
            g.storeLocal(slot);
        }
        g.mark(after);
    }

    private static void emitConstants(ClassWriter cw, RuleClassData data) {
        for (int i = 0; i < data.size(); i++) {
            cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, RuleClassData.fieldName(i), data.type(i).getDescriptor(), null, null)
                    .visitEnd();
        }
        final GeneratorAdapter g = new GeneratorAdapter(ACC_STATIC, Method.getMethod("void <clinit>()"), null, null, cw);
        g.visitCode();
        if (data.size() > 0) {
            g.invokeStatic(METHOD_HANDLES, LOOKUP);
            g.push(ConstantDescs.DEFAULT_NAME);
            g.push(LIST);
            g.invokeStatic(METHOD_HANDLES, CLASS_DATA);
            g.checkCast(LIST);
            final int list = g.newLocal(LIST);
            g.storeLocal(list);
            for (int i = 0; i < data.size(); i++) {
                g.loadLocal(list);
                g.push(i);
                g.invokeInterface(LIST, LIST_GET);
                g.checkCast(data.type(i));
                g.putStatic(OWNER, RuleClassData.fieldName(i), data.type(i));
            }
        }
        g.returnValue();
        g.endMethod();
    }

    private static GeneratorAdapter method(ClassWriter cw, String descriptor) {
        final Method method = Method.getMethod(descriptor);
        final GeneratorAdapter g = new GeneratorAdapter(ACC_PUBLIC, method,
                new CodeSizeGuard(cw.visitMethod(ACC_PUBLIC, method.getName(), method.getDescriptor(), null, null)));
        g.visitCode();
        return g;
    }
}
