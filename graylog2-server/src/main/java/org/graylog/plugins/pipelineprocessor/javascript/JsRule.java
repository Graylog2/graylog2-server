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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class JsRule {
    private final Source source;
    private final Supplier<Context> polyglotContextSupplier;
    private final FunctionRegistry functionRegistry;

    private final AtomicReference<Context> polyglotContext = new AtomicReference<>();
    private final AtomicReference<EvaluatedRule> evaluatedRule = new AtomicReference<>();

    // TODO: this effectively synchronizes all pipeline processors on this single lock and will become a choke point
    //   This needs to go away!!
    //   We should probably have one context per thread instead
    private final ReentrantLock contextLock = new ReentrantLock();

    private record EvaluatedRule(Value name, Value when, Value then) {}

    public JsRule(Source source, Supplier<Context> polyglotContextSupplier, FunctionRegistry functionRegistry) {
        this.source = source;
        this.polyglotContextSupplier = polyglotContextSupplier;
        this.functionRegistry = functionRegistry;
    }

    public void initialize() {
        final Context context = polyglotContextSupplier.get();
        context.initialize("js");
        polyglotContext.set(context);
        evaluatedRule.set(withinPolyglotContext(() -> evaluate(source, context)));
    }

    public void shutDown() {
        final var context = polyglotContext.get();
        if (context != null) {
            context.close();
        }
    }

    public String name() {
        return getRule().name().asString();
    }

    public Boolean when(EvaluationContext pipelineEvaluationContext) {
        final var messageProxy = new MessageProxy(pipelineEvaluationContext.currentMessage());
        final var functionsProxy = new PipelineFunctionProxy(functionRegistry, pipelineEvaluationContext);
        return withinPolyglotContext(() -> getRule().when().execute(messageProxy, functionsProxy).asBoolean());
    }

    public Void then(EvaluationContext pipelineEvaluationContext) {
        final var messageProxy = new MessageProxy(pipelineEvaluationContext.currentMessage());
        final var functionsProxy = new PipelineFunctionProxy(functionRegistry, pipelineEvaluationContext);
        withinPolyglotContext(() -> getRule().then().execute(messageProxy, functionsProxy));
        return null;
    }

    private <T> T withinPolyglotContext(Supplier<T> toBeWrapped) {
        final var ctxt = getPolyglotContext();
        contextLock.lock();
        try {
            try {
                ctxt.enter();
                return toBeWrapped.get();
            } finally {
                ctxt.leave();
            }
        } finally {
            contextLock.unlock();
        }
    }

    private EvaluatedRule getRule() {
        final var rule = evaluatedRule.get();
        if (rule == null) {
            throw new IllegalStateException("Rule is not initialized yet. Call #initialize before using it.");
        }
        return rule;
    }

    private Context getPolyglotContext() {
        final var context = polyglotContext.get();
        if (context == null) {
            throw new IllegalStateException("Rule is not initialized yet. Call #initialize before using it.");
        }
        return context;
    }

    private EvaluatedRule evaluate(Source source, Context context) {
        final Value result = context.eval(source);

        final Value defaultExport = result.getMember("default");

        final Value name = defaultExport.getMember("name");
        final Value when = defaultExport.getMember("when");
        final Value then = defaultExport.getMember("then");
        return new EvaluatedRule(name, when, then);
    }
}
