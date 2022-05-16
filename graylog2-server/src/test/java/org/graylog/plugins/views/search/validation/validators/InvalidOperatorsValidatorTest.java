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
package org.graylog.plugins.views.search.validation.validators;

import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.search.engine.QueryPosition;
import org.graylog.plugins.views.search.validation.QueryValidator;
import org.graylog.plugins.views.search.validation.TestValidationContext;
import org.graylog.plugins.views.search.validation.ValidationContext;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;

class InvalidOperatorsValidatorTest {


    private QueryValidator sut;

    @BeforeEach
    void setUp() {
        sut = new InvalidOperatorsValidator();
    }

    @Test
    void testLowercaseAndPosition() {

        final ValidationContext context = TestValidationContext.create("foo:bar and")
                .build();

        final List<ValidationMessage> validation = sut.validate(context);

        assertThat(validation.size()).isEqualTo(1);

        final ValidationMessage message = validation.iterator().next();
        assertThat(message.validationType()).isEqualTo(ValidationType.INVALID_OPERATOR);
        assertThat(message.relatedProperty()).hasValue("and");

        assertThat(message.position()).hasValue(QueryPosition.builder()
                .beginLine(1)
                .beginColumn(8)
                .endLine(1)
                .endColumn(11)
                .build());
    }


    @Test
    void testInvalidOperatorNoOperatorPresent() {
        final ValidationContext context = TestValidationContext.create("foo:bar baz")
                .build();
        assertThat(sut.validate(context)).isEmpty();
    }

    @Test
    void testInvalidOperatorLowercaseAnd() {
        final ValidationContext context = TestValidationContext.create("foo:bar and")
                .build();
        final List<ValidationMessage> messages = sut.validate(context);
        assertThat(messages.size()).isEqualTo(1);
        final ValidationMessage message = messages.iterator().next();
        assertThat(message.validationType()).isEqualTo(ValidationType.INVALID_OPERATOR);
        assertThat(message.relatedProperty()).hasValue("and");
    }


    @Test
    void testInvalidOperatorLowercaseOr() {
        final ValidationContext context = TestValidationContext.create("foo:bar or")
                .build();
        final List<ValidationMessage> messages = sut.validate(context);
        assertThat(messages.size()).isEqualTo(1);
        final ValidationMessage message = messages.iterator().next();
        assertThat(message.validationType()).isEqualTo(ValidationType.INVALID_OPERATOR);
        assertThat(message.relatedProperty()).hasValue("or");
    }

    @Test
    void testLowercaseNegation() {
        final ValidationContext context = TestValidationContext.create("not(foo:bar)")
                .build();
        final List<ValidationMessage> messages = sut.validate(context);
        assertThat(messages.size()).isEqualTo(1);
        final ValidationMessage message = messages.iterator().next();
        assertThat(message.validationType()).isEqualTo(ValidationType.INVALID_OPERATOR);
        assertThat(message.relatedProperty()).hasValue("not");
    }

    @Test
    void testRepeatedInvalidTokens() {
        final ValidationContext context = TestValidationContext.create("not(foo:bar)")
                .build();
        final List<ValidationMessage> messages = sut.validate(context);
        assertThat(messages.size()).isEqualTo(1);
        assertThat(messages.stream().allMatch(v -> v.validationType() == ValidationType.INVALID_OPERATOR)).isTrue();
    }


    @Test
    void testLongStringOfInvalidTokens() {
        final ValidationContext context = TestValidationContext.create("and and and or or or")
                .build();
        final List<ValidationMessage> messages = sut.validate(context);
        assertThat(messages.size()).isEqualTo(6);
        assertThat(messages.stream().allMatch(v -> v.validationType() == ValidationType.INVALID_OPERATOR)).isTrue();

        Assertions.assertThat(messages)
                .extracting(v -> v.relatedProperty().orElse("invalid-property"))
                .containsOnly("and", "or");
    }
}
