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
package org.graylog.events.conditions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.revinate.assertj.json.JsonPathAssert;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BooleanNumberConditionsVisitorTest {
    private static ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @BeforeClass
    public static void classSetUp() {
        Configuration.setDefaults(new Configuration.Defaults() {
            private final JsonProvider jsonProvider = new JacksonJsonProvider(objectMapper);
            private final MappingProvider mappingProvider = new JacksonMappingProvider(objectMapper);

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }
        });
    }

    @Test
    public void testTrue() throws Exception {
        assertThat(Expr.True.create().accept(new BooleanNumberConditionsVisitor())).isTrue();

        assertThat(loadCondition("condition-true.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testAnd() throws Exception {
        final Expr.Greater trueExpr = Expr.Greater.create(Expr.NumberValue.create(2), Expr.NumberValue.create(1));
        final Expr.Greater falseExpr = Expr.Greater.create(Expr.NumberValue.create(1), Expr.NumberValue.create(2));

        assertThat(Expr.And.create(trueExpr, trueExpr).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.And.create(trueExpr, falseExpr).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(Expr.And.create(falseExpr, trueExpr).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(Expr.And.create(falseExpr, falseExpr).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(loadCondition("condition-and.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testOr() throws Exception {
        final Expr.Greater trueExpr = Expr.Greater.create(Expr.NumberValue.create(2), Expr.NumberValue.create(1));
        final Expr.Greater falseExpr = Expr.Greater.create(Expr.NumberValue.create(1), Expr.NumberValue.create(2));

        assertThat(Expr.Or.create(trueExpr, trueExpr).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.Or.create(trueExpr, falseExpr).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.Or.create(falseExpr, trueExpr).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.Or.create(falseExpr, falseExpr).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(loadCondition("condition-or.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testNot() throws Exception {
        final Expr.Greater trueExpr = Expr.Greater.create(Expr.NumberValue.create(2), Expr.NumberValue.create(1));
        final Expr.Greater falseExpr = Expr.Greater.create(Expr.NumberValue.create(1), Expr.NumberValue.create(2));

        assertThat(Expr.Not.create(falseExpr).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.Not.create(trueExpr).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(loadCondition("condition-not.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testEqual() throws Exception {
        assertThat(Expr.Equal.create(
                Expr.NumberValue.create(2), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.Equal.create(
                Expr.NumberValue.create(1), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(Expr.Equal.create(
                Expr.NumberValue.create(2), Expr.NumberValue.create(1)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(loadCondition("condition-equal.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testGreater() throws Exception {
        assertThat(Expr.Greater.create(
                Expr.NumberValue.create(2), Expr.NumberValue.create(1)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.Greater.create(
                Expr.NumberValue.create(1), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(Expr.Greater.create(
                Expr.NumberValue.create(2), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(loadCondition("condition-greater.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testGreaterEqual() throws Exception {
        assertThat(Expr.GreaterEqual.create(
                Expr.NumberValue.create(2), Expr.NumberValue.create(1)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.GreaterEqual.create(
                Expr.NumberValue.create(2), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.GreaterEqual.create(
                Expr.NumberValue.create(1), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(loadCondition("condition-greater-equal.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testLesser() throws Exception {
        assertThat(Expr.Lesser.create(
                Expr.NumberValue.create(1), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.Lesser.create(
                Expr.NumberValue.create(2), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(Expr.Lesser.create(
                Expr.NumberValue.create(3), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(loadCondition("condition-lesser.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testLesserEqual() throws Exception {
        assertThat(Expr.LesserEqual.create(
                Expr.NumberValue.create(1), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.LesserEqual.create(
                Expr.NumberValue.create(2), Expr.NumberValue.create(2)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isTrue();

        assertThat(Expr.LesserEqual.create(
                Expr.NumberValue.create(2), Expr.NumberValue.create(1)
        ).accept(new BooleanNumberConditionsVisitor()))
                .isFalse();

        assertThat(loadCondition("condition-lesser-equal.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testNumberReference() throws Exception {
        final String ref = "abc123";

        assertThat(Expr.Equal.create(
                Expr.NumberReference.create(ref),
                Expr.NumberValue.create(42)
        ).accept(new BooleanNumberConditionsVisitor(Collections.singletonMap(ref, 42d))))
                .isTrue();

        assertThatThrownBy(() -> Expr.Equal.create(
                Expr.NumberReference.create(ref),
                Expr.NumberValue.create(42)).accept(new BooleanNumberConditionsVisitor()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Expr.Equal.create(
                Expr.NumberReference.create("abc"),
                Expr.NumberValue.create(42)).accept(new BooleanNumberConditionsVisitor(Collections.singletonMap(ref, 42d))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(loadCondition("condition-number-reference.json")
                .accept(new BooleanNumberConditionsVisitor(Collections.singletonMap("foo", 2d))))
                .isTrue();
    }

    @Test
    public void testCombined() throws Exception {
        assertThat(loadCondition("condition-combined.json").accept(new BooleanNumberConditionsVisitor()))
                .isTrue();
    }

    @Test
    public void testGroupedEvaluation() throws  Exception {
        // [count() >= 10 AND count() < 100 AND count() > 20] OR [count() == 101 OR count() == 402 OR [count() > 200 AND count() < 300]]
        final Expression<Boolean> condition = loadCondition("condition-grouped.json");
        final String ref = "count-";

        assertThat(condition.accept(new BooleanNumberConditionsVisitor(Collections.singletonMap(ref, 42d)))).isTrue();
        assertThat(condition.accept(new BooleanNumberConditionsVisitor(Collections.singletonMap(ref, 8d)))).isFalse();

        assertThat(condition.accept(new BooleanNumberConditionsVisitor(Collections.singletonMap(ref, 402d)))).isTrue();
        assertThat(condition.accept(new BooleanNumberConditionsVisitor(Collections.singletonMap(ref, 403d)))).isFalse();

        assertThat(condition.accept(new BooleanNumberConditionsVisitor(Collections.singletonMap(ref, 250d)))).isTrue();
        assertThat(condition.accept(new BooleanNumberConditionsVisitor(Collections.singletonMap(ref, 300d)))).isFalse();
    }

    @Test
    public void testSerialization() throws Exception {
        final Expr.NumberValue left = Expr.NumberValue.create(2);
        final Expr.NumberReference right = Expr.NumberReference.create("the-ref");

        assertJsonPath(Expr.Equal.create(left, right), assertThat -> {
            assertThat.jsonPathAsString("$.expr").isEqualTo("==");
            assertLeftAndRight(assertThat, left, right);
        });

        assertJsonPath(Expr.Greater.create(left, right), assertThat -> {
            assertThat.jsonPathAsString("$.expr").isEqualTo(">");
            assertLeftAndRight(assertThat, left, right);
        });

        assertJsonPath(Expr.GreaterEqual.create(left, right), assertThat -> {
            assertThat.jsonPathAsString("$.expr").isEqualTo(">=");
            assertLeftAndRight(assertThat, left, right);
        });

        assertJsonPath(Expr.Lesser.create(left, right), assertThat -> {
            assertThat.jsonPathAsString("$.expr").isEqualTo("<");
            assertLeftAndRight(assertThat, left, right);
        });

        assertJsonPath(Expr.LesserEqual.create(left, right), assertThat -> {
            assertThat.jsonPathAsString("$.expr").isEqualTo("<=");
            assertLeftAndRight(assertThat, left, right);
        });

        assertJsonPath(Expr.And.create(Expr.Equal.create(left, right), Expr.Equal.create(left, right)), assertThat -> {
            assertThat.jsonPathAsString("$.expr").isEqualTo("&&");

            assertThat.jsonPathAsString("$.left.expr").isEqualTo("==");
            assertThat.jsonPathAsString("$.left.left.expr").isEqualTo("number");
            assertThat.jsonPathAsString("$.left.right.expr").isEqualTo("number-ref");

            assertThat.jsonPathAsString("$.right.expr").isEqualTo("==");
            assertThat.jsonPathAsString("$.right.left.expr").isEqualTo("number");
            assertThat.jsonPathAsString("$.right.right.expr").isEqualTo("number-ref");
        });

        assertJsonPath(Expr.Or.create(Expr.Equal.create(left, right), Expr.Equal.create(left, right)), assertThat -> {
            assertThat.jsonPathAsString("$.expr").isEqualTo("||");

            assertThat.jsonPathAsString("$.left.expr").isEqualTo("==");
            assertThat.jsonPathAsString("$.left.left.expr").isEqualTo("number");
            assertThat.jsonPathAsString("$.left.right.expr").isEqualTo("number-ref");

            assertThat.jsonPathAsString("$.right.expr").isEqualTo("==");
            assertThat.jsonPathAsString("$.right.left.expr").isEqualTo("number");
            assertThat.jsonPathAsString("$.right.right.expr").isEqualTo("number-ref");
        });

        assertJsonPath(Expr.Not.create(Expr.Equal.create(left, right)), assertThat -> {
            assertThat.jsonPathAsString("$.expr").isEqualTo("!");

            assertThat.jsonPathAsString("$.left.expr").isEqualTo("==");
            assertThat.jsonPathAsString("$.left.left.expr").isEqualTo("number");
            assertThat.jsonPathAsString("$.left.right.expr").isEqualTo("number-ref");
        });

        assertJsonPath(Expr.Group.create(Expr.Equal.create(left, right), "&&"), assertThat -> {
            assertThat.jsonPathAsString("$.expr").isEqualTo("group");
            assertThat.jsonPathAsString("$.operator").isEqualTo("&&");
            assertThat.jsonPathAsString("$.child.expr").isEqualTo("==");

        });
    }

    private void assertJsonPath(Expression<Boolean> expression, Consumer<JsonPathAssert> consumer) throws Exception {
        final DocumentContext context = JsonPath.parse(objectMapper.writeValueAsString(expression));
        final JsonPathAssert jsonPathAssert = JsonPathAssert.assertThat(context);

        consumer.accept(jsonPathAssert);
    }

    private void assertLeftAndRight(JsonPathAssert at, Expr.NumberValue left, Expr.NumberReference right) {
        at.jsonPathAsString("$.left.expr").isEqualTo("number");
        at.jsonPathAsBigDecimal("$.left.value").isEqualTo(BigDecimal.valueOf(left.value()));
        at.jsonPathAsString("$.right.expr").isEqualTo("number-ref");
        at.jsonPathAsString("$.right.ref").isEqualTo(right.ref());
    }

    private Expression<Boolean> loadCondition(String filename) throws IOException {
        final URL resource = Resources.getResource(getClass(), filename);
        return objectMapper.readValue(resource, new TypeReference<Expression<Boolean>>() {
        });
    }
}
