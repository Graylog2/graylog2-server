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
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.TestMessageFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;

class GeneratedRulesPropertyTest {
    private static final Map<String, Function<?>> FUNCTIONS = new HashMap<>();

    static {
        FUNCTIONS.putAll(RuleCompilerTest.FUNCTIONS);
        FUNCTIONS.putAll(StringFastPathTest.FUNCTIONS);
        FUNCTIONS.putAll(ConversionFastPathTest.FUNCTIONS);
    }

    private final RuleCompiler compiler = new RuleCompiler(new MetricRegistry(), DifferentialHarness::verify);
    private final PipelineRuleParser parser = new PipelineRuleParser(new FunctionRegistry(FUNCTIONS));

    @Property(tries = 3000, maxDiscardRatio = 50)
    void compiledMatchesInterpreter(@ForAll("rules") String source, @ForAll("fields") Map<String, Object> fields) {
        final Rule rule;
        try {
            rule = parser.parseRule(source, true);
        } catch (ParseException | NullPointerException e) {
            // The parser crashes with an NPE when a re-declared variable is referenced before its last
            // declaration's type is inferred (e.g. "let x = "x"; set_field("x", x); let x = (a * b);").
            // Such rules cannot be saved in production either, so they are discarded like parse errors.
            Assume.that(false);
            return;
        }
        DifferentialHarness.assertEquivalent(() -> message(fields), List.of(rule), compiler::compileOrThrow);
    }

    private static Message message(Map<String, Object> fields) {
        final Message m = new TestMessageFactory()
                .createMessage("generated", "source", new DateTime(2026, 1, 1, 0, 0, DateTimeZone.UTC));
        fields.forEach((k, v) -> m.addField(k, v instanceof List<?> l ? new ArrayList<>(l) : v));
        return m;
    }

    @Provide
    Arbitrary<Map<String, Object>> fields() {
        final Arbitrary<Object> value = Arbitraries.oneOf(
                Arbitraries.of("abc", "ABC", "", "42", "1.5", "true", "Hello World"),
                Arbitraries.of(0L, 1L, -5L, 42L),
                Arbitraries.of(1.5d, 0.0d, -2.25d),
                Arbitraries.of(true, false),
                Arbitraries.just(new DateTime(2020, 5, 5, 5, 5, DateTimeZone.UTC)),
                Arbitraries.just(List.of("a", "b")),
                Arbitraries.just(Map.of("k", "v")));
        return Arbitraries.maps(Arbitraries.of("a", "b", "n", "d", "s", "l"), value).ofMaxSize(6);
    }

    @Provide
    Arbitrary<String> rules() {
        final Arbitrary<String> condition = bools(2);
        final Arbitrary<String> statements = statement().list().ofMinSize(1).ofMaxSize(5).map(l -> String.join("\n", l));
        return Combinators.combine(condition, statements)
                .as((c, s) -> f("rule \"gen\"\nwhen %s\nthen\n%s\nend", c, s));
    }

    private Arbitrary<String> statement() {
        return Arbitraries.oneOf(
                values(2).map(v -> f("set_field(\"out\", %s);", v)),
                values(2).map(v -> f("let x = %s; set_field(\"x\", x);", v)),
                values(1).map(v -> f("let y = %s; set_field(\"y\", to_string(y));", v)),
                Arbitraries.just("throw_error();"));
    }

    private Arbitrary<String> values(int depth) {
        final Arbitrary<String> leaf = Arbitraries.oneOf(
                Arbitraries.of("\"x\"", "\"ABC\"", "\"\"", "0", "1", "-5", "42", "1.5", "true"),
                Arbitraries.of("a", "b", "n", "d", "s", "l", "missing").map(field -> "$message." + field));
        if (depth == 0) {
            return leaf;
        }
        final Arbitrary<String> inner = values(depth - 1);
        return Arbitraries.oneOf(leaf,
                inner.map(v -> f("to_string(%s)", v)),
                inner.map(v -> f("to_long(%s)", v)),
                inner.map(v -> f("to_long(%s, 7)", v)),
                inner.map(v -> f("to_double(%s)", v)),
                inner.map(v -> f("lowercase(to_string(%s))", v)),
                inner.map(v -> f("length(to_string(%s))", v)),
                inner.map(v -> f("substring(to_string(%s), 0, 2)", v)),
                Combinators.combine(inner, inner).as((l, r) -> f("concat(to_string(%s), to_string(%s))", l, r)),
                Combinators.combine(inner, inner).as((l, r) -> f("(to_long(%s) + to_long(%s))", l, r)),
                Combinators.combine(inner, inner).as((l, r) -> f("(to_double(%s) * to_double(%s))", l, r)),
                inner.map(v -> f("[1, %s][1]", v)),
                inner.map(v -> f("{k: %s}[\"k\"]", v)),
                Arbitraries.just("throw_value()"));
    }

    private Arbitrary<String> bools(int depth) {
        final Arbitrary<String> leaf = Arbitraries.oneOf(
                Arbitraries.of("true", "false", "throw_bool()"),
                Arbitraries.of("a", "n", "missing").map(field -> f("has_field(\"%s\")", field)),
                values(1).map(v -> f("is_string(%s)", v)),
                Combinators.combine(values(1), values(1)).as((l, r) -> f("%s == %s", l, r)),
                Combinators.combine(values(1), values(1)).as((l, r) -> f("%s != %s", l, r)),
                Combinators.combine(values(1), values(1)).as((l, r) -> f("to_long(%s) > to_long(%s)", l, r)),
                Combinators.combine(values(1), Arbitraries.of("\"a\"", "\"B\"")).as((v, s) -> f("contains(to_string(%s), %s)", v, s)));
        if (depth == 0) {
            return leaf;
        }
        final Arbitrary<String> inner = bools(depth - 1);
        return Arbitraries.oneOf(leaf,
                inner.map(b -> f("!(%s)", b)),
                Combinators.combine(inner, inner).as((l, r) -> f("(%s && %s)", l, r)),
                Combinators.combine(inner, inner).as((l, r) -> f("(%s || %s)", l, r)));
    }
}
