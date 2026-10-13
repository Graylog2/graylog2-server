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
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.conversion.DoubleConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.LongConversion;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NativeEmissionTest {
    private final MetricRegistry metrics = new MetricRegistry();
    private final RuleCompiler compiler = new RuleCompiler(metrics, DifferentialHarness::verify);
    private final PipelineRuleParser parser = new PipelineRuleParser(new FunctionRegistry(functions()));

    private static Map<String, Function<?>> functions() {
        final Map<String, Function<?>> functions = new HashMap<>(RuleCompilerTest.FUNCTIONS);
        functions.put(LongConversion.NAME, new LongConversion());
        functions.put(DoubleConversion.NAME, new DoubleConversion());
        return functions;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "rule \"r\" when true && !false then set_field(\"a\", \"b\"); end",
            "rule \"r\" when false || has_field(\"num\") then set_field(\"a\", $message.num); end",
            "rule \"r\" when !(has_field(\"nope\") || false) then set_field(\"a\", 1.5); end",
            "rule \"r\" when true then let x = \"lit\"; let y = 7; let z = $message.message; let w = $message; end",
            "rule \"r\" when has_field(\"num\") && throw_bool() then set_field(\"a\", 1); end"
    })
    void nativeConstructsMatchInterpreterWithoutEscapes(String source) {
        final Rule rule = parser.parseRule(source, false);
        DifferentialHarness.assertEquivalent(RuleCompilerTest::input, List.of(rule), compiler::compileOrThrow);
        assertThat(metrics.getCounters().keySet())
                .noneMatch(n -> n.startsWith(MetricRegistry.name(RuleCompiler.class, "escaped")));
    }

    /**
     * The type checker requires identical operand types, so numeric fields go through {@code to_long}/{@code to_double}
     * (fast paths, no escapes); Object-vs-Object operands exercise the runtime CCE/NPE paths.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "rule \"r\" when to_long($message.num) > 40 then let v = to_long($message.num) + 1; set_field(\"a\", v); end",
            // CCE: (long) "hello"
            "rule \"r\" when $message.num >= $message.message then set_field(\"a\", 1); end",
            // NPE: (long) null
            "rule \"r\" when $message.missing < $message.num then set_field(\"a\", 1); end",
            "rule \"r\" when $message.message == \"hello\" then let v = 2 * 3 - 1; set_field(\"a\", v); end",
            "rule \"r\" when $message.missing == \"x\" then set_field(\"a\", 1); end",
            "rule \"r\" when $message.message != \"nope\" then let v = -$message.num; set_field(\"a\", v); end",
            // ArithmeticException
            "rule \"r\" when true then let v = to_long($message.num) / 0; set_field(\"a\", v); end",
            "rule \"r\" when true then let v = \"x\" + to_string($message.message); set_field(\"a\", v); end",
            "rule \"r\" when true then let v = 7 % 3; let w = 1.5 * 2.0; set_field(\"a\", v); set_field(\"b\", w); end",
            "rule \"r\" when true then let v = to_long($message.num) % 5; let w = to_double($message.num) * 1.5; "
                    + "set_field(\"a\", v); set_field(\"b\", w); end",
            // Object-typed addition takes the double branch: CCE (double) Long
            "rule \"r\" when true then let v = $message.num + $message.num; set_field(\"a\", v); end",
            "rule \"r\" when true then let b = $message.message == \"hello\"; let c = to_double($message.num) <= 1.5; "
                    + "set_field(\"a\", b); set_field(\"b\", c); end"
    })
    void operatorsMatchInterpreterWithoutEscapes(String source) {
        final Rule rule = parser.parseRule(source, false);
        DifferentialHarness.assertEquivalent(RuleCompilerTest::input, List.of(rule), compiler::compileOrThrow);
        assertThat(metrics.getCounters().keySet())
                .noneMatch(n -> n.startsWith(MetricRegistry.name(RuleCompiler.class, "escaped")));
    }
}
