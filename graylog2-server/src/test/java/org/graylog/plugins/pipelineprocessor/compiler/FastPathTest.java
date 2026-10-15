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
import static org.graylog2.shared.utilities.StringUtils.f;

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

    /** Two params, both resolved unconditionally in order by evaluate(); subclasses differ only in the annotation. */
    public abstract static class Join extends AbstractFunction<String> {
        private final ParameterDescriptor<Object, String> firstParam = ParameterDescriptor.object("first", String.class).build();
        private final ParameterDescriptor<Object, String> secondParam = ParameterDescriptor.object("second", String.class).optional().build();
        private final String name;

        Join(String name) {
            this.name = name;
        }

        @Override
        public String evaluate(FunctionArgs args, EvaluationContext context) {
            return join(firstParam.required(args, context), secondParam.optional(args, context).orElse(null));
        }

        public abstract String join(String first, String second);

        static String joined(String first, String second) {
            return first.toUpperCase(Locale.ROOT) + "+" + second;
        }

        @Override
        public FunctionDescriptor<String> descriptor() {
            return FunctionDescriptor.<String>builder().name(name).returnType(String.class)
                    .params(firstParam, secondParam).build();
        }
    }

    public static class FirstEagerJoin extends Join {
        public FirstEagerJoin() {
            super("first_eager_join");
        }

        @Override
        @PipelineFastPath
        public String join(String first, String second) {
            return joined(first, second);
        }
    }

    public static class EagerJoin extends Join {
        public EagerJoin() {
            super("eager_join");
        }

        @Override
        @PipelineFastPath(eagerParams = 2)
        public String join(String first, String second) {
            return joined(first, second);
        }
    }

    public static class SecondAbsentJoin extends Join {
        public SecondAbsentJoin() {
            super("second_absent_join");
        }

        @Override
        @PipelineFastPath(eagerParams = 2, requiresAbsent = "second")
        public String join(String first, String second) {
            return joined(first, second);
        }
    }

    public static class TooEagerJoin extends Join {
        public TooEagerJoin() {
            super("too_eager_join");
        }

        @Override
        @PipelineFastPath(eagerParams = 3)
        public String join(String first, String second) {
            return joined(first, second);
        }
    }

    public static class NegativeEagerJoin extends Join {
        public NegativeEagerJoin() {
            super("negative_eager_join");
        }

        @Override
        @PipelineFastPath(eagerParams = -1)
        public String join(String first, String second) {
            return joined(first, second);
        }
    }

    public static class UnknownAbsentJoin extends Join {
        public UnknownAbsentJoin() {
            super("unknown_absent_join");
        }

        @Override
        @PipelineFastPath(requiresAbsent = "third")
        public String join(String first, String second) {
            return joined(first, second);
        }
    }

    /** Legacy function with an observable side effect: counts its calls and marks the message. */
    public static class Tick extends AbstractFunction<Object> {
        private final ParameterDescriptor<Object, Object> valueParam = ParameterDescriptor.object("value").build();
        private int calls;

        @Override
        public Object evaluate(FunctionArgs args, EvaluationContext context) {
            calls++;
            context.currentMessage().addField("ticked", true);
            return valueParam.required(args, context);
        }

        @Override
        public FunctionDescriptor<Object> descriptor() {
            return FunctionDescriptor.builder().name("tick").returnType(Object.class).params(valueParam).build();
        }
    }

    private final MetricRegistry metrics = new MetricRegistry();
    private final RuleCompiler compiler = new RuleCompiler(metrics, DifferentialHarness::verify);
    private final Tick tick = new Tick();
    private final PipelineRuleParser parser;

    FastPathTest() {
        final Map<String, Function<?>> functions = new HashMap<>(RuleCompilerTest.FUNCTIONS);
        functions.put("shout", new Shout());
        functions.put("bad_shout", new BadShout());
        for (Join join : List.of(new FirstEagerJoin(), new EagerJoin(), new SecondAbsentJoin(), new TooEagerJoin(),
                new NegativeEagerJoin(), new UnknownAbsentJoin())) {
            functions.put(join.descriptor().name(), join);
        }
        functions.put("tick", tick);
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

    /** Calls under test go into a native {@code let}. */
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
        assertEquivalent("rule \"r\" when true then tick(shout($message.message)); end");
        assertThat(calls("legacy", "tick")).isEqualTo(1);
        assertThat(calls("fastPath", "shout")).isZero();
        assertThat(calls("legacy", "shout")).isZero();
    }

    @Test
    void disabledFastPathsUseLegacyPath() {
        final RuleCompiler withoutFastPaths = new RuleCompiler(metrics, DifferentialHarness::verify, false);
        final Rule rule = parser.parseRule("rule \"r\" when true then let s = shout($message.message); set_field(\"x\", s); end", false);
        DifferentialHarness.assertEquivalent(FastPathTest::input, List.of(rule), withoutFastPaths::compileOrThrow);
        assertThat(calls("fastPath", "shout")).isZero();
        assertThat(calls("legacy", "shout")).isEqualTo(1);
    }

    @Test
    void mismatchedSignatureFallsBackToLegacy() {
        assertThatThrownBy(() -> FastPathResolver.validate(new BadShout())).isInstanceOf(IllegalStateException.class);
        assertEquivalent("rule \"r\" when true then let s = bad_shout($message.message); set_field(\"x\", s); end");
        assertThat(calls("legacy", "bad_shout")).isEqualTo(1);
    }

    @Test
    void eagerPrefixAllowsRuntimeLaterArguments() {
        assertEquivalent("rule \"r\" when true then let s = eager_join($message.message, $message.other); set_field(\"x\", s); end");
        assertEquivalent("rule \"r\" when true then let s = first_eager_join($message.message, $message.other); set_field(\"x\", s); end");
        assertThat(calls("fastPath", "eager_join")).isEqualTo(1);
        assertThat(calls("fastPath", "first_eager_join")).isZero();
        assertThat(calls("legacy", "first_eager_join")).isEqualTo(1);
    }

    @Test
    void eagerArgumentsAreEvaluatedInOrder() {
        assertEquivalent("rule \"r\" when true then let s = eager_join($message.message, tick($message.other)); set_field(\"x\", s); end");
        assertThat(calls("fastPath", "eager_join")).isEqualTo(1);
        // one call per engine
        assertThat(tick.calls).isEqualTo(2);
    }

    @Test
    void firstArgumentFailureSkipsLaterArguments() {
        assertEquivalent("rule \"r\" when true then let s = eager_join(throw_value(), tick($message.other)); set_field(\"x\", s); end");
        assertThat(calls("fastPath", "eager_join")).isEqualTo(1);
        assertThat(tick.calls).isZero();
    }

    @Test
    void firstArgumentConversionFailureSkipsLaterArguments() {
        // `num` is a Long, the param converts to String: the ClassCastException happens before `second` is evaluated
        assertEquivalent("rule \"r\" when true then let s = eager_join($message.num, tick($message.other)); set_field(\"x\", s); end");
        assertThat(calls("fastPath", "eager_join")).isEqualTo(1);
        assertThat(tick.calls).isZero();
    }

    @Test
    void requiresAbsentParamMustBeAbsent() {
        assertEquivalent("rule \"r\" when true then let s = second_absent_join($message.message, \"const\"); set_field(\"x\", s); end");
        assertThat(calls("legacy", "second_absent_join")).isEqualTo(1);
        assertThat(calls("fastPath", "second_absent_join")).isZero();
        assertEquivalent("rule \"r\" when true then let s = second_absent_join($message.message); set_field(\"x\", s); end");
        assertThat(calls("fastPath", "second_absent_join")).isEqualTo(1);
    }

    @Test
    void invalidEagerAnnotationsAreRejected() {
        assertThatThrownBy(() -> FastPathResolver.validate(new TooEagerJoin()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("eagerParams");
        assertThatThrownBy(() -> FastPathResolver.validate(new NegativeEagerJoin()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("eagerParams");
        assertThatThrownBy(() -> FastPathResolver.validate(new UnknownAbsentJoin()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("requiresAbsent");
        for (String name : List.of("too_eager_join", "negative_eager_join", "unknown_absent_join")) {
            assertEquivalent(f("rule \"r\" when true then let s = %s($message.message); set_field(\"x\", s); end", name));
            assertThat(calls("legacy", name)).as(name).isEqualTo(1);
            assertThat(calls("fastPath", name)).as(name).isZero();
        }
    }
}
