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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import org.antlr.v4.runtime.CommonToken;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.StringExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeyValueTest {

    private static KeyValue classUnderTest;
    private static EvaluationContext evaluationContext;
    public static StringExpression valueExpression;

    @BeforeAll
    static void setUp() {
        classUnderTest = new KeyValue();
        final Message message = new Message("__dummy", "__dummy", DateTime.parse("2010-07-30T16:03:25Z"));
        evaluationContext = new EvaluationContext(message);
        valueExpression = new StringExpression(new CommonToken(0), "test=do test=remi number=12345 i get ignored");
    }

    @Test
    void testDefaultSettingsTakeFirst() {
        final Map<String, Expression> arguments = Collections.singletonMap("value", valueExpression);

        Map<String, String> result = classUnderTest.evaluate(new FunctionArgs(classUnderTest, arguments), evaluationContext);

        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("test", "do");
        expectedResult.put("number", "12345");

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedResult);
    }

    @Test
    void testTakeLast() {
        final Map<String, Expression> arguments = Map.of("value", valueExpression, "handle_dup_keys",
                new StringExpression(new CommonToken(0), "TAKE_LAST"));

        Map<String, String> result = classUnderTest.evaluate(new FunctionArgs(classUnderTest, arguments), evaluationContext);

        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("test", "remi");
        expectedResult.put("number", "12345");

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedResult);
    }

    @Test
    void testConcatDemiliter() {
        final Map<String, Expression> arguments = Map.of("value", valueExpression, "handle_dup_keys",
                new StringExpression(new CommonToken(0), ","));

        Map<String, String> result = classUnderTest.evaluate(new FunctionArgs(classUnderTest, arguments), evaluationContext);

        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("test", "do,remi");
        expectedResult.put("number", "12345");

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedResult);
    }

    @Test
    void testDisableDuplicateKeys() {
        final Map<String, Expression> arguments = Map.of("value", valueExpression, "allow_dup_keys",
                new BooleanExpression(new CommonToken(0), false));

        Assertions.assertThrows(IllegalArgumentException.class, () -> classUnderTest.evaluate(new FunctionArgs(classUnderTest, arguments), evaluationContext));
    }


    @Test
    void testDisableIgnoreEmptyValues() {
        final Map<String, Expression> arguments = Map.of("value", valueExpression, "ignore_empty_values",
                new BooleanExpression(new CommonToken(0), false));

        Assertions.assertThrows(IllegalArgumentException.class, () -> classUnderTest.evaluate(new FunctionArgs(classUnderTest, arguments), evaluationContext));
    }


    @Test
    void testTrimCharacters() {
        final Map<String, Expression> arguments = Map.of("value", valueExpression, "trim_key_chars",
                new StringExpression(new CommonToken(0), "t"), "trim_value_chars", new StringExpression(new CommonToken(0), "d"));

        Map<String, String> result = classUnderTest.evaluate(new FunctionArgs(classUnderTest, arguments), evaluationContext);

        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("es", "o");
        expectedResult.put("number", "12345");

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedResult);
    }

    @Test
    void testValueSplitParam() {
        final Map<String, Expression> arguments = Map.of("value", new StringExpression(new CommonToken(0),
                "test:do test:remi number:12345 i get=ignored"), "kv_delimiters", new StringExpression(new CommonToken(0), ":"));

        Map<String, String> result = classUnderTest.evaluate(new FunctionArgs(classUnderTest, arguments), evaluationContext);

        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("test", "do");
        expectedResult.put("number", "12345");

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedResult);
    }
}
