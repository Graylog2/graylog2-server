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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.TestMessageFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleCompilerTest {
    static final Map<String, Function<?>> FUNCTIONS = new HashMap<>();

    static {
        FUNCTIONS.put(SetField.NAME, new SetField());
        FUNCTIONS.put(HasField.NAME, new HasField());
        FUNCTIONS.put(StringConversion.NAME, new StringConversion());
        FUNCTIONS.put("throw_error", testFunction("throw_error", Void.class));
        FUNCTIONS.put("throw_bool", testFunction("throw_bool", Boolean.class));
        FUNCTIONS.put("throw_value", testFunction("throw_value", Object.class));
    }

    private final MetricRegistry metrics = new MetricRegistry();
    private final RuleCompiler compiler = new RuleCompiler(metrics, DifferentialHarness::verify);
    private final PipelineRuleParser parser = new PipelineRuleParser(new FunctionRegistry(FUNCTIONS));

    static <T> Function<T> testFunction(String name, Class<T> type) {
        return new AbstractFunction<>() {
            @Override
            public T evaluate(FunctionArgs args, EvaluationContext context) {
                throw new IllegalStateException(name + " always fails");
            }

            @Override
            public FunctionDescriptor<T> descriptor() {
                return FunctionDescriptor.<T>builder().name(name).returnType(type).params(ImmutableList.of()).build();
            }
        };
    }

    static Message input() {
        final Message m = new TestMessageFactory().createMessage("hello", "source", new DateTime(2026, 1, 1, 0, 0, DateTimeZone.UTC));
        m.addField("num", 42L);
        return m;
    }

    private void assertCompiledEquivalent(String source) {
        final Rule rule = parser.parseRule(source, false);
        DifferentialHarness.assertEquivalent(RuleCompilerTest::input, List.of(rule), compiler::compileOrThrow);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "rule \"r\" when true then set_field(\"a\", \"b\"); end",
            "rule \"r\" when false then set_field(\"a\", \"b\"); end",
            "rule \"r\" when has_field(\"num\") then let x = to_string($message.num); set_field(\"s\", x); end",
            "rule \"r\" when true then set_field(\"a\", \"1\"); throw_error(); set_field(\"b\", \"2\"); end",
            "rule \"r\" when throw_bool() then set_field(\"a\", \"b\"); end",
            "rule \"r\" when true then let x = \"v\"; let y = x; set_field(\"y\", y); end",
            "rule \"r\" when true then let x = throw_value(); set_field(\"x\", to_string(x)); end",
            "rule \"r\" when true then end",
            // a bare $message has a null child node
            "rule \"r\" when has_field(\"num\", $message) then set_field(\"a\", \"b\"); end"
    })
    void compiledRulesMatchInterpreter(String source) {
        assertCompiledEquivalent(source);
    }

    @ParameterizedTest
    @ValueSource(strings = {"quote\\\"s", "a/b c", "emoji 🚀", "dots.and$dollars", "x"})
    void oddRuleNamesCompile(String name) {
        assertCompiledEquivalent("rule \"" + name + "\" when true then set_field(\"a\", \"b\"); end");
    }

    @Test
    void hugeRuleFallsBackCleanly() {
        final StringBuilder source = new StringBuilder("rule \"huge\" when true then\n");
        for (int i = 0; i < 6000; i++) {
            source.append("set_field(\"f").append(i).append("\", \"v\");\n");
        }
        final Rule rule = parser.parseRule(source.append("end").toString(), false);
        assertThat(compiler.compile(rule)).isEmpty();
        assertThat(metrics.getCounters().keySet()).anyMatch(n -> n.startsWith(MetricRegistry.name(RuleCompiler.class, "fallback")));
        DifferentialHarness.assertEquivalent(RuleCompilerTest::input, List.of(rule), r -> compiler.compile(r).orElse(null));
    }
}
