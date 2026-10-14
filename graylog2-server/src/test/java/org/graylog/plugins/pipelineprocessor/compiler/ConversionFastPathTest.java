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
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.conversion.BooleanConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.DoubleConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsBoolean;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsCollection;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsDouble;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsList;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsLong;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsMap;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsNumber;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsString;
import org.graylog.plugins.pipelineprocessor.functions.conversion.LongConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.MapConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.TestMessageFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionFastPathTest {
    static final Map<String, Function<?>> FUNCTIONS = new HashMap<>();

    static {
        FUNCTIONS.put("str", new StringFastPathTest.Raw("str", String.class));
        FUNCTIONS.put("lng", new StringFastPathTest.Raw("lng", Long.class));
        FUNCTIONS.put("dbl", new StringFastPathTest.Raw("dbl", Double.class));
        FUNCTIONS.put(SetField.NAME, new SetField());
        FUNCTIONS.put(StringConversion.NAME, new StringConversion());
        FUNCTIONS.put(LongConversion.NAME, new LongConversion());
        FUNCTIONS.put(DoubleConversion.NAME, new DoubleConversion());
        FUNCTIONS.put(BooleanConversion.NAME, new BooleanConversion());
        FUNCTIONS.put(MapConversion.NAME, new MapConversion());
        FUNCTIONS.put(IsString.NAME, new IsString());
        FUNCTIONS.put(IsLong.NAME, new IsLong());
        FUNCTIONS.put(IsDouble.NAME, new IsDouble());
        FUNCTIONS.put(IsNumber.NAME, new IsNumber());
        FUNCTIONS.put(IsBoolean.NAME, new IsBoolean());
        FUNCTIONS.put(IsCollection.NAME, new IsCollection());
        FUNCTIONS.put(IsList.NAME, new IsList());
        FUNCTIONS.put(IsMap.NAME, new IsMap());
    }

    private final MetricRegistry metrics = new MetricRegistry();
    private final RuleCompiler compiler = new RuleCompiler(metrics, DifferentialHarness::verify);
    private final PipelineRuleParser parser = new PipelineRuleParser(new FunctionRegistry(FUNCTIONS));

    static Message input() {
        // to_string($message) embeds the id, which otherwise differs between the interpreted and the compiled run
        final Message m = new TestMessageFactory().createMessage("fixed-id", StringFastPathTest.input().getFields());
        final Map<String, Object> map = new HashMap<>();
        map.put("a", 1L);
        m.addField("map", map);
        m.addField("dbl", 1.5d);
        m.addField("flag", true);
        return m;
    }

    private long calls(String kind, String function) {
        final var counter = metrics.getCounters().get(MetricRegistry.name(RuleCompiler.class, "call", kind, function));
        return counter == null ? 0 : counter.getCount();
    }

    /** The call goes into a native {@code let}: arguments of the legacy set_field are AST-evaluated and never reach a fast path. */
    private void assertEquivalent(String call) {
        final String source = "rule \"r\" when true then let v = " + call + "; set_field(\"out\", v); end";
        DifferentialHarness.assertEquivalent(ConversionFastPathTest::input, List.of(parser.parseRule(source, false)), compiler::compileOrThrow);
    }

    private static String functionName(String call) {
        return call.substring(0, call.indexOf('('));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "to_string($message.num)", "to_string($message.missing)", "to_string($message.missing, \"dflt\")",
            "to_string($message.missing, \"dflt\", true)", "to_string($message.list)", "to_string($message)",
            "to_string($message.map)", "to_string($message.timestamp)",
            "to_long($message.num)", "to_long(\"12\")", "to_long($message.message)", "to_long($message.message, 7)",
            "to_long($message.missing, 7, true)", "to_long($message.missing, 7)", "to_long($message.dbl)",
            "to_double($message.num)", "to_double(\"1.5\")", "to_double($message.missing, 2.5)",
            "to_double($message.message)", "to_double($message.missing, 2.5, true)",
            "to_bool(\"true\")", "to_bool($message.missing)", "to_bool($message.missing, true)",
            "to_bool($message.missing, true, true)", "to_bool($message.flag)",
            "is_string($message.message)", "is_string($message.missing)",
            "is_long($message.num)", "is_long($message.message, true)", "is_long(\"12\", true)",
            "is_double($message.num)", "is_double($message.dbl)", "is_double(\"1.5\", true)", "is_double($message.message, true)",
            "is_number($message.num)", "is_number($message.message)", "is_number(\"1e3\", true)",
            "is_number($message.missing, true)",
            "is_bool($message.message)", "is_bool($message.flag)", "is_bool(\"yes\", true)", "is_bool($message.message, true)",
            "is_collection($message.list)", "is_collection($message.map)", "is_list($message.list)", "is_list($message.missing)",
            "is_map($message)", "is_map($message.map)",
            "to_map($message.list)", "to_map($message.map)", "to_map($message.missing)", "to_map($message.missing, true)",
            // eager value and default: a runtime default stays on the fast path
            "to_long($message.message, lng($message.map.a))", "to_double($message.missing, dbl($message.dbl))"
    })
    void fastPathMatchesInterpreter(String call) {
        assertEquivalent(call);
        assertThat(calls("fastPath", functionName(call))).as(call).isEqualTo(1);
    }

    @Test
    void laterNonConstantArgumentUsesLegacyPath() {
        // `message` is non-null, so the interpreter never evaluates `default` and its Long->String CCE never happens
        assertEquivalent("to_string($message.message, str($message.num))");
        assertThat(calls("legacy", StringConversion.NAME)).isEqualTo(1);
    }
}
