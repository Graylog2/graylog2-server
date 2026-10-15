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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.arrays.ArrayContains;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RemoveSingleField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RenameField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetFields;
import org.graylog.plugins.pipelineprocessor.functions.strings.Lowercase;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Calls are used directly as statements or as set_field values: set_field's own arguments are native positions. */
class MessageFastPathTest {
    static final Map<String, Function<?>> FUNCTIONS = new HashMap<>();

    static {
        FUNCTIONS.putAll(RuleCompilerTest.FUNCTIONS);
        FUNCTIONS.put("str", new StringFastPathTest.Raw("str", String.class));
        FUNCTIONS.put("list", new StringFastPathTest.Raw("list", List.class));
        FUNCTIONS.put("map", new StringFastPathTest.Raw("map", Map.class));
        FUNCTIONS.put(StringConversion.NAME, new StringConversion());
        FUNCTIONS.put(Lowercase.NAME, new Lowercase());
        FUNCTIONS.put(SetField.NAME, new SetField());
        FUNCTIONS.put(HasField.NAME, new HasField());
        FUNCTIONS.put(RenameField.NAME, new RenameField());
        FUNCTIONS.put(SetFields.NAME, new SetFields());
        FUNCTIONS.put(RemoveSingleField.NAME, new RemoveSingleField());
        FUNCTIONS.put(RegexMatch.NAME, new RegexMatch());
        FUNCTIONS.put(ArrayContains.NAME, new ArrayContains(new ObjectMapper()));
    }

    private final MetricRegistry metrics = new MetricRegistry();
    private final RuleCompiler compiler = new RuleCompiler(metrics, DifferentialHarness::verify);
    private final PipelineRuleParser parser = new PipelineRuleParser(new FunctionRegistry(FUNCTIONS));

    static Message input() {
        final Message m = StringFastPathTest.input();
        m.addField("fields", Map.of("a", "1", "b", 2L));
        m.addField("bad_keys", Map.of("a b", "1"));
        m.addField("num_keys", Map.of(1L, "x"));
        m.addField("bad_regex", "(");
        m.addField("numbers", List.of(1L, 2L));
        return m;
    }

    private long calls(String kind, String function) {
        final var counter = metrics.getCounters().get(MetricRegistry.name(RuleCompiler.class, "call", kind, function));
        return counter == null ? 0 : counter.getCount();
    }

    private void assertEquivalent(String when, String then) {
        final String source = "rule \"r\" when " + when + " then " + then + "; end";
        DifferentialHarness.assertEquivalent(MessageFastPathTest::input, List.of(parser.parseRule(source, false)), compiler::compileOrThrow);
    }

    private static String functionName(String call) {
        return call.substring(0, call.indexOf('('));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "set_field(\"x\", \"v\")", "set_field(\"x\", $message.num)", "set_field(\"x\", $message.missing)",
            "set_field(\"\", \"v\")", "set_field(str($message.missing), \"v\")", "set_field(str($message.num), \"v\")",
            "set_field(str($message.message), \"v\")", "set_field(\"x\", \"v\", \"pre_\", \"_suf\")",
            "set_field(field: \"x\", value: \"v\", prefix: \"pre_\")", "set_field(field: \"x\", value: \"v\", suffix: \"_suf\")",
            "set_field(\"x y\", \"v\")", "set_field(field: \"x y\", value: \"v\", clean_field: true)",
            "set_field(field: \"x y\", value: \"v\", prefix: \"p q \", clean_field: false)",
            "set_field(\"x\", throw_value())", "set_field(\"\", throw_value())",
            "rename_field(\"message\", \"renamed\")", "rename_field(\"message\", \"message\")",
            "rename_field(\"missing\", \"renamed\")", "rename_field(str($message.missing), \"renamed\")",
            "rename_field(\"num\", str($message.missing))", "rename_field(str($message.empty), str($message.empty))",
            "rename_field(str($message.num), \"renamed\")",
            "remove_single_field(\"num\")", "remove_single_field(\"missing\")", "remove_single_field(str($message.missing))",
            "set_fields({a: \"1\", b: 2})", "set_fields({a: \"1\"}, \"pre_\", \"_suf\")", "set_fields(map($message.fields))",
            "set_fields(map($message.missing))", "set_fields(map($message.bad_keys))",
            "set_fields(fields: map($message.bad_keys), clean_fields: true)", "set_fields(map($message.num_keys))",
            "set_fields(map($message.message))"
    })
    void statementFastPathMatchesInterpreter(String statement) {
        assertEquivalent("true", statement);
        assertThat(calls("fastPath", functionName(statement))).as(statement).isEqualTo(1);
    }

    /** Without a let: the call is an argument of the set_field fast path, so it is emitted natively too. */
    @ParameterizedTest
    @ValueSource(strings = {
            "has_field(\"num\")", "has_field(\"missing\")", "has_field(str($message.missing))", "has_field(\"num\", $message)",
            "has_field(str($message.num))",
            "regex(\"(\\\\w+) (\\\\w+)\", str($message.message))", "regex(\"(\\\\w+)\", str($message.message), [\"first\"])",
            "regex(\"(x)?(H)\", str($message.message))", "regex(\"x\", str($message.missing))",
            "regex(str($message.bad_regex), str($message.message))", "regex(str($message.missing), str($message.message))",
            "regex(\"x\", str($message.num))",
            "array_contains($message.list, \"a\")", "array_contains($message.list, \"A\")",
            "array_contains($message.list, \"A\", true)", "array_contains($message.list, \"zzz\")",
            "array_contains($message.missing, \"a\")", "array_contains($message.numbers, $message.num)",
            "array_contains($message.numbers, 2)", "array_contains($message.message, \"a\")",
            "array_contains([1, 2], $message.missing)", "array_contains($message.list, str($message.message))"
    })
    void valueFastPathMatchesInterpreter(String call) {
        assertEquivalent("true", "set_field(\"out\", " + call + ")");
        assertThat(calls("fastPath", functionName(call))).as(call).isEqualTo(1);
        assertThat(calls("fastPath", SetField.NAME)).as(call).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"has_field(\"num\")", "has_field(\"missing\")", "array_contains($message.list, \"a\")"})
    void conditionFastPathMatchesInterpreter(String condition) {
        assertEquivalent(condition, "set_field(\"matched\", true)");
        assertThat(calls("fastPath", functionName(condition))).as(condition).isEqualTo(1);
    }

    @Test
    void nestedCallsInSetFieldUseFastPaths() {
        assertEquivalent("true", "set_field(\"x\", lowercase(to_string($message.message)))");
        assertThat(calls("fastPath", SetField.NAME)).isEqualTo(1);
        assertThat(calls("fastPath", Lowercase.NAME)).isEqualTo(1);
        assertThat(calls("fastPath", StringConversion.NAME)).isEqualTo(1);
    }

    /** A present default, or a runtime argument after the eager prefix, keeps the call on the legacy path. */
    @ParameterizedTest
    @ValueSource(strings = {
            "set_field(field: \"x\", value: \"v\", default: \"d\")", "set_field(field: \"x\", value: throw_value(), default: \"d\")",
            "set_field(field: \"x\", value: $message.missing, default: \"d\")", "set_field(\"x\", \"v\", str($message.empty))",
            "set_field(field: \"x\", value: \"v\", message: $message)", "rename_field(\"message\", \"renamed\", $message)",
            "set_fields({a: \"1\"}, str($message.empty))", "set_fields(fields: {a: \"1\"}, clean_fields: has_field(\"num\"))"
    })
    void laterRuntimeArgumentUsesLegacyPath(String statement) {
        assertEquivalent("true", statement);
        assertThat(calls("legacy", functionName(statement))).as(statement).isEqualTo(1);
    }

    @Test
    void runtimeGroupNamesUseLegacyRegex() {
        assertEquivalent("true", "set_field(\"out\", regex(\"(\\\\w+)\", str($message.message), list($message.list)))");
        assertThat(calls("legacy", RegexMatch.NAME)).isEqualTo(1);
    }
}
