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
import com.google.common.io.Resources;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetFields;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.TestMessageFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The rule of the former code generator's fixture, which uses field access and both collection literals. */
class CodegenFixtureTest {
    private static final Map<String, Function<?>> FUNCTIONS = Map.of(
            StringConversion.NAME, new StringConversion(),
            RegexMatch.NAME, new RegexMatch(),
            HasField.NAME, new HasField(),
            SetFields.NAME, new SetFields());

    private final RuleCompiler compiler = new RuleCompiler(new MetricRegistry(), DifferentialHarness::verify);
    private final PipelineRuleParser parser = new PipelineRuleParser(new FunctionRegistry(FUNCTIONS));

    private static Message jenkinsMessage() {
        // matches the rule's "#\d+" regex
        return new TestMessageFactory().createMessage("Started by upstream project \"graylog\" build number #1234",
                "jenkins.torch.sh", new DateTime(2026, 1, 1, 0, 0, DateTimeZone.UTC));
    }

    @Test
    void codegenFixtureMatchesInterpreter() throws IOException {
        final String source = Resources.toString(
                Resources.getResource(CodegenFixtureTest.class, "/org/graylog/plugins/pipelineprocessor/codegen/runCodegen.txt"),
                StandardCharsets.UTF_8);
        final Rule rule = parser.parseRule(source, false);

        DifferentialHarness.assertEquivalent(CodegenFixtureTest::jenkinsMessage, List.of(rule), compiler::compileOrThrow);

        final DifferentialHarness.Outcome compiled = DifferentialHarness.run(List.of(rule), jenkinsMessage(), compiler::compileOrThrow);
        // the condition matched; "something with spaces" is not a valid message field name, so set_fields skips it
        assertThat(compiled.message().fields()).containsEntry("some_identifier", "java.lang.Long:1");
    }
}
