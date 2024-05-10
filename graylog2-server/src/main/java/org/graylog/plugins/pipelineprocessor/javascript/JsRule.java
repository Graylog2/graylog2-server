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

import java.util.function.Supplier;

public class JsRule {
    private final Source source;
    private final Supplier<Context> graalContextSupplier;

    private record DefaultExport(Value name, Value when, Value then) {}

    public JsRule(Source source, Supplier<Context> graalContextSupplier) {
        this.source = source;
        this.graalContextSupplier = graalContextSupplier;
    }

    public String name() {
        try (final Context ctx = graalContextSupplier.get()) {
            return evaluate(source, ctx).name().asString();
        }
    }

    public Boolean when(EvaluationContext context) {
        try (final Context ctx = graalContextSupplier.get()) {
            return evaluate(source, ctx).when().execute(context.currentMessage()).asBoolean();
        }
    }

    public Object then(EvaluationContext context) {
        try (final Context ctx = graalContextSupplier.get()) {
            evaluate(source, ctx).then().execute(context.currentMessage());
        }
        return null;
    }

    private DefaultExport evaluate(Source source, Context context) {
        final Value result = context.eval(source);

        final Value defaultExport = result.getMember("default");

        final Value name = defaultExport.getMember("name");
        final Value when = defaultExport.getMember("when");
        final Value then = defaultExport.getMember("then");
        return new DefaultExport(name, when, then);
    }
}
