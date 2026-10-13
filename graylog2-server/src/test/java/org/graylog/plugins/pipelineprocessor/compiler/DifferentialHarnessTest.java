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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DifferentialHarnessTest {
    private static final MessageFactory MESSAGES = new TestMessageFactory();
    private final PipelineRuleParser parser = new PipelineRuleParser(new FunctionRegistry(
            Map.<String, Function<?>>of(SetField.NAME, new SetField())));

    private static Message input() {
        return MESSAGES.createMessage("hello", "source", new DateTime(2026, 1, 1, 0, 0, DateTimeZone.UTC));
    }

    @Test
    void fallbackRunIsEquivalent() {
        final Rule rule = parser.parseRule("rule \"r\" when true then set_field(\"a\", \"b\"); end", false);
        assertThatCode(() -> DifferentialHarness.assertEquivalent(DifferentialHarnessTest::input, List.of(rule), r -> null))
                .doesNotThrowAnyException();
    }

    @Test
    void detectsDivergingCompiledBody() {
        final Rule rule = parser.parseRule("rule \"r\" when true then set_field(\"a\", \"b\"); end", false);
        final CompiledRule wrong = new CompiledRule() {
            @Override
            public boolean when(EvaluationContext ctx) {
                return true;
            }

            @Override
            public boolean then(EvaluationContext ctx) {
                ctx.currentMessage().addField("a", "c");
                return true;
            }
        };
        assertThatThrownBy(() -> DifferentialHarness.assertEquivalent(DifferentialHarnessTest::input, List.of(rule), r -> wrong))
                .hasMessageContaining("DIFFERENTIAL MISMATCH");
    }

    @Test
    void detectsDivergingValueType() {
        final Rule rule = parser.parseRule("rule \"r\" when true then set_field(\"a\", 1); end", false);
        final CompiledRule wrongType = new CompiledRule() {
            @Override
            public boolean when(EvaluationContext ctx) {
                return true;
            }

            @Override
            public boolean then(EvaluationContext ctx) {
                ctx.currentMessage().addField("a", 1); // Integer, not Long
                return true;
            }
        };
        assertThatThrownBy(() -> DifferentialHarness.assertEquivalent(DifferentialHarnessTest::input, List.of(rule), r -> wrongType))
                .hasMessageContaining("DIFFERENTIAL MISMATCH");
    }
}
