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
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.PipelineFastPath;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FastPathTest {
    /**
     * Eager function: evaluate() resolves all params in order and delegates to the fast path.
     * Params are typed {@code Object} so the parser accepts message fields; the String check happens at runtime.
     */
    public static class Shout extends AbstractFunction<String> {
        private final ParameterDescriptor<Object, String> valueParam = ParameterDescriptor.object("value", String.class).build();
        private final ParameterDescriptor<Object, String> suffixParam = ParameterDescriptor.object("suffix", String.class).optional().build();

        @Override
        public String evaluate(FunctionArgs args, EvaluationContext context) {
            return apply(valueParam.required(args, context), suffixParam.optional(args, context).orElse(null));
        }

        @PipelineFastPath
        public String apply(String value, String suffix) {
            return value.toUpperCase(Locale.ROOT) + (suffix == null ? "!" : suffix);
        }

        @Override
        public FunctionDescriptor<String> descriptor() {
            return FunctionDescriptor.<String>builder().name("shout").returnType(String.class)
                    .params(valueParam, suffixParam).build();
        }
    }

    /** Annotated method does not match the descriptor: must be rejected and treated as legacy. */
    public static class BadShout extends Shout {
        @PipelineFastPath
        public String apply(Long value) {
            return "bad";
        }

        @Override
        public FunctionDescriptor<String> descriptor() {
            return FunctionDescriptor.<String>builder().name("bad_shout").returnType(String.class)
                    .params(ParameterDescriptor.object("value", String.class).build()).build();
        }
    }

    private final MetricRegistry metrics = new MetricRegistry();
    private final RuleCompiler compiler = new RuleCompiler(metrics, DifferentialHarness::verify);
    private final PipelineRuleParser parser;

    FastPathTest() {
        final Map<String, Function<?>> functions = new HashMap<>(RuleCompilerTest.FUNCTIONS);
        functions.put("shout", new Shout());
        functions.put("bad_shout", new BadShout());
        parser = new PipelineRuleParser(new FunctionRegistry(functions));
    }

    private static Message input() {
        final Message m = RuleCompilerTest.input();
        m.addField("other", "?");
        return m;
    }

    private long calls(String kind, String function) {
        final var counter = metrics.getCounters().get(MetricRegistry.name(RuleCompiler.class, "call", kind, function));
        return counter == null ? 0 : counter.getCount();
    }

    /** set_field is legacy, so its arguments are AST-evaluated (see callNestedInLegacyCallIsAstEvaluated): calls under test go into a native {@code let}. */
    private void assertEquivalent(String source) {
        final Rule rule = parser.parseRule(source, false);
        DifferentialHarness.assertEquivalent(FastPathTest::input, List.of(rule), compiler::compileOrThrow);
    }

    @Test
    void fastPathUsedForEligibleCall() {
        assertEquivalent("rule \"r\" when true then let s = shout($message.message); set_field(\"x\", s); end");
        assertEquivalent("rule \"r\" when true then let s = shout($message.message, \"?\"); set_field(\"x\", s); end");
        assertThat(calls("fastPath", "shout")).isEqualTo(2);
    }

    @Test
    void laterNonConstantArgumentUsesLegacyPath() {
        assertEquivalent("rule \"r\" when true then let s = shout($message.message, $message.other); set_field(\"x\", s); end");
        assertThat(calls("fastPath", "shout")).isZero();
        assertThat(calls("legacy", "shout")).isEqualTo(1);
    }

    @Test
    void typeMismatchErrorTextMatchesInterpreter() {
        // `num` is a Long: Class.cast in ParameterDescriptor.convert must produce the same message in both engines
        assertEquivalent("rule \"r\" when true then let s = shout($message.num); set_field(\"x\", s); end");
        assertThat(calls("fastPath", "shout")).isEqualTo(1);
    }

    @Test
    void nestedFastPathErrorKeepsInnerLocation() {
        assertEquivalent("rule \"r\" when true then let s = shout(shout($message.num)); set_field(\"x\", s); end");
        assertThat(calls("fastPath", "shout")).isEqualTo(2);
    }

    /**
     * Pins a design limit: a legacy call evaluates its arguments through the AST, so a fast-path function nested in
     * one is never emitted and takes neither counter. Fast paths only apply at natively emitted positions.
     */
    @Test
    void callNestedInLegacyCallIsAstEvaluated() {
        assertEquivalent("rule \"r\" when true then set_field(\"x\", shout($message.message)); end");
        assertThat(calls("legacy", "set_field")).isEqualTo(1);
        assertThat(calls("fastPath", "shout")).isZero();
        assertThat(calls("legacy", "shout")).isZero();
    }

    @Test
    void mismatchedSignatureFallsBackToLegacy() {
        assertThatThrownBy(() -> FastPathResolver.validate(new BadShout())).isInstanceOf(IllegalStateException.class);
        assertEquivalent("rule \"r\" when true then let s = bad_shout($message.message); set_field(\"x\", s); end");
        assertThat(calls("legacy", "bad_shout")).isEqualTo(1);
    }
}
