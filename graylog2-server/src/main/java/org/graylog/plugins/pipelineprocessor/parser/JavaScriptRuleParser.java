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
package org.graylog.plugins.pipelineprocessor.parser;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.javascript.JsRule;
import org.graylog.plugins.pipelineprocessor.javascript.JsRuleParseError;
import org.graylog.plugins.pipelineprocessor.javascript.JsRuleStatement;
import org.graylog.plugins.pipelineprocessor.javascript.JsRuleWhenExpression;
import org.graylog.plugins.pipelineprocessor.javascript.MessageProxy;
import org.graylog2.plugin.Message;

import java.io.IOException;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.graylog.plugins.pipelineprocessor.parser.RuleContentType.JAVASCRIPT_MODULE;

public class JavaScriptRuleParser {

    private final Context.Builder contextBuilder;
    private final FunctionRegistry functionRegistry;

    public JavaScriptRuleParser(FunctionRegistry functionRegistry) {
        this.functionRegistry = functionRegistry;
        contextBuilder = jsContextBuilder();
    }

    public Rule parseRule(String id, String source, boolean silent) throws ParseException {

        try {
            return doParse(id, source);
        } catch (IOException e) {
            throw new ParseException(Set.of(new JsRuleParseError(e)));
        }
    }

    private Rule doParse(String id, String source) throws IOException {

        final Source parsedSource = Source.newBuilder("js", source, "main.js")
                .mimeType(JAVASCRIPT_MODULE.mimeType())
                .build();

        final JsRule jsRule = new JsRule(parsedSource, contextBuilder::build, functionRegistry);

        return Rule.builder()
                .id(id)
                .name(jsRule.name())
                .when(new JsRuleWhenExpression(jsRule))
                .then(singleton(new JsRuleStatement(jsRule)))
                .shutDownHook(jsRule::shutDown)
                .build();
    }

    private Context.Builder jsContextBuilder() {
        final Engine engine = Engine.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false").build();
        return Context.newBuilder("js")
                .engine(engine)
                .allowHostAccess(HostAccess.newBuilder()
                        .allowAccessAnnotatedBy(HostAccess.Export.class)
                        .targetTypeMapping(Value.class, Message.class, Value::isProxyObject,
                                value -> ((MessageProxy) value.asProxyObject()).getDelegate())
                        .build())
                .allowHostClassLoading(false)
                .allowHostClassLookup(className -> false)
                .allowIO(IOAccess.NONE)
                .option("js.foreign-object-prototype", "true")
                .allowExperimentalOptions(true)
                .option("js.esm-eval-returns-exports", "true");
    }
}

