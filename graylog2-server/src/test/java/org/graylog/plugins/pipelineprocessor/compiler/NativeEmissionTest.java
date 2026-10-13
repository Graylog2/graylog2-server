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
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NativeEmissionTest {
    private final MetricRegistry metrics = new MetricRegistry();
    private final RuleCompiler compiler = new RuleCompiler(metrics, DifferentialHarness::verify);
    private final PipelineRuleParser parser = new PipelineRuleParser(new FunctionRegistry(RuleCompilerTest.FUNCTIONS));

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
}
