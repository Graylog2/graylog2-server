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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class JsRule {
    private final Source source;
    private final Supplier<Context> polyglotContextSupplier;
    private final FunctionRegistry functionRegistry;

    private final ThreadLocal<EvaluatedRule> evaluatedRule = new ThreadLocal<>();
    private final CopyOnWriteArrayList<Context> openContexts = new CopyOnWriteArrayList<>();

    private record EvaluatedRule(Value name, Value when, Value then) {}

    public JsRule(Source source, Supplier<Context> polyglotContextSupplier, FunctionRegistry functionRegistry) {
        this.source = source;
        this.polyglotContextSupplier = polyglotContextSupplier;
        this.functionRegistry = functionRegistry;
    }

    private void initialize() {
        final Context context = polyglotContextSupplier.get();
        context.initialize("js");
        evaluatedRule.set(evaluate(source, context));
        openContexts.add(context);
    }

    public void shutDown() {
        openContexts.forEach(Context::close);
    }

    public String name() {
        return getRule().name().asString();
    }

    public Boolean when(EvaluationContext pipelineEvaluationContext) {
        final var messageProxy = new MessageProxy(pipelineEvaluationContext.currentMessage());
        final var functionsProxy = new PipelineFunctionProxy(functionRegistry, pipelineEvaluationContext);
        return getRule().when().execute(messageProxy, functionsProxy).asBoolean();
    }

    public Void then(EvaluationContext pipelineEvaluationContext) {
        final var messageProxy = new MessageProxy(pipelineEvaluationContext.currentMessage());
        final var functionsProxy = new PipelineFunctionProxy(functionRegistry, pipelineEvaluationContext);
        getRule().then().execute(messageProxy, functionsProxy);
        return null;
    }

    private EvaluatedRule getRule() {
        if (evaluatedRule.get() == null) {
            initialize();
        }
        return evaluatedRule.get();
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
