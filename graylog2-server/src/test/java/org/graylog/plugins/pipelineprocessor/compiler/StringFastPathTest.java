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
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.strings.Abbreviate;
import org.graylog.plugins.pipelineprocessor.functions.strings.Capitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Concat;
import org.graylog.plugins.pipelineprocessor.functions.strings.Contains;
import org.graylog.plugins.pipelineprocessor.functions.strings.EndsWith;
import org.graylog.plugins.pipelineprocessor.functions.strings.FirstNonNull;
import org.graylog.plugins.pipelineprocessor.functions.strings.Length;
import org.graylog.plugins.pipelineprocessor.functions.strings.Lowercase;
import org.graylog.plugins.pipelineprocessor.functions.strings.Replace;
import org.graylog.plugins.pipelineprocessor.functions.strings.Split;
import org.graylog.plugins.pipelineprocessor.functions.strings.StartsWith;
import org.graylog.plugins.pipelineprocessor.functions.strings.Substring;
import org.graylog.plugins.pipelineprocessor.functions.strings.Swapcase;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uncapitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uppercase;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StringFastPathTest {
    static final Map<String, Function<?>> FUNCTIONS = new HashMap<>();

    /**
     * Returns its argument unchanged but declares {@code type} as return type: message fields are typed {@code Object},
     * which the parser rejects for typed parameters, and {@code to_string} would hide nulls and wrong types.
     */
    public static class Raw extends AbstractFunction<Object> {
        private final ParameterDescriptor<Object, Object> valueParam = ParameterDescriptor.object("value").build();
        private final String name;
        private final Class<?> type;

        Raw(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public Object evaluate(FunctionArgs args, EvaluationContext context) {
            return valueParam.required(args, context);
        }

        @Override
        public FunctionDescriptor<Object> descriptor() {
            return FunctionDescriptor.builder().name(name).returnType(type).params(valueParam).build();
        }
    }

    static {
        FUNCTIONS.put("str", new Raw("str", String.class));
        FUNCTIONS.put("list", new Raw("list", List.class));
        FUNCTIONS.put(SetField.NAME, new SetField());
        FUNCTIONS.put(Lowercase.NAME, new Lowercase());
        FUNCTIONS.put(Uppercase.NAME, new Uppercase());
        FUNCTIONS.put(Capitalize.NAME, new Capitalize());
        FUNCTIONS.put(Uncapitalize.NAME, new Uncapitalize());
        FUNCTIONS.put(Swapcase.NAME, new Swapcase());
        FUNCTIONS.put(Abbreviate.NAME, new Abbreviate());
        FUNCTIONS.put(Contains.NAME, new Contains());
        FUNCTIONS.put(StartsWith.NAME, new StartsWith());
        FUNCTIONS.put(EndsWith.NAME, new EndsWith());
        FUNCTIONS.put(Substring.NAME, new Substring());
        FUNCTIONS.put(Concat.NAME, new Concat());
        FUNCTIONS.put(Replace.NAME, new Replace());
        FUNCTIONS.put(Split.NAME, new Split());
        FUNCTIONS.put(Length.NAME, new Length());
        FUNCTIONS.put(FirstNonNull.NAME, new FirstNonNull());
    }

    private final MetricRegistry metrics = new MetricRegistry();
    private final RuleCompiler compiler = new RuleCompiler(metrics, DifferentialHarness::verify);
    private final PipelineRuleParser parser = new PipelineRuleParser(new FunctionRegistry(FUNCTIONS));

    static Message input() {
        final Message m = RuleCompilerTest.input();
        m.addField("message", "Hello World");
        m.addField("empty", "");
        m.addField("list", Arrays.asList("a", null));
        return m;
    }

    private long calls(String kind, String function) {
        final var counter = metrics.getCounters().get(MetricRegistry.name(RuleCompiler.class, "call", kind, function));
        return counter == null ? 0 : counter.getCount();
    }

    /** The call goes into a native {@code let}: arguments of the legacy set_field are AST-evaluated and never reach a fast path. */
    private void assertEquivalent(String call) {
        final String source = "rule \"r\" when true then let v = " + call + "; set_field(\"out\", v); end";
        DifferentialHarness.assertEquivalent(StringFastPathTest::input, List.of(parser.parseRule(source, false)), compiler::compileOrThrow);
    }

    private static String functionName(String call) {
        return call.substring(0, call.indexOf('('));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "lowercase(str($message.message))", "lowercase(str($message.message), \"tr\")", "lowercase(str($message.num))",
            "lowercase(str($message.missing))",
            "uppercase(str($message.message))", "capitalize(str($message.empty))", "uncapitalize(str($message.message))",
            "swapcase(str($message.message))",
            "abbreviate(str($message.message), 5)", "abbreviate(str($message.missing), 5)",
            "contains(str($message.message), \"world\")", "contains(str($message.message), \"world\", true)",
            "contains(str($message.missing), \"x\")",
            "starts_with(str($message.message), \"Hell\")", "starts_with(str($message.message), \"hell\", true)",
            "ends_with(str($message.message), \"World\")", "ends_with(str($message.message), \"world\", true)",
            "substring(str($message.message), 0, 5)", "substring(str($message.message), 6)", "substring(str($message.message), -5)",
            "substring(str($message.missing), 0)",
            "concat(str($message.message), \"!\")", "concat(str($message.missing), \"!\")",
            "replace(str($message.message), \"o\", \"0\")", "replace(str($message.message), \"o\")",
            "replace(str($message.message), \"o\", \"0\", 1)", "replace(str($message.missing), \"o\", \"0\")",
            "split(str($message.empty), \"a b\")", "split(str($message.missing), \"a b\")", "split(\" \", \"a b c\", 2)",
            "length(str($message.message))", "length(str($message.message), true)", "length(str($message.missing))",
            "first_non_null(list($message.list))", "first_non_null(list($message.missing))",
            // eager functions: runtime arguments after the first stay on the fast path
            "contains(str($message.message), str($message.empty))", "concat(str($message.message), str($message.missing))",
            "concat(str($message.num), str($message.message))", "replace(str($message.message), str($message.empty), str($message.message))",
            "lowercase(str($message.message), str($message.empty))"
    })
    void fastPathMatchesInterpreter(String call) {
        assertEquivalent(call);
        assertThat(calls("fastPath", functionName(call))).as(call).isEqualTo(1);
    }

    /** A runtime argument after the first one keeps a lazy function's call on the legacy path (Refinement 1). */
    @ParameterizedTest
    @ValueSource(strings = {
            "split(\" \", str($message.message))", "substring(str($message.message), 0, length(str($message.empty)))"
    })
    void laterRuntimeArgumentUsesLegacyPath(String call) {
        assertEquivalent(call);
        assertThat(calls("legacy", functionName(call))).as(call).isEqualTo(1);
    }
}
