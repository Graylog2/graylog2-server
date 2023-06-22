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
package org.graylog.plugins.views.search.validation;

import org.apache.lucene.queryparser.classic.QueryParserConstants;
import org.apache.lucene.queryparser.classic.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FieldTypeValidationTest {

    private FieldTypeValidation fieldTypeValidation;

    @BeforeEach
    void setUp() {
        fieldTypeValidation = new FieldTypeValidationImpl();
    }

    @Test
    void validateNumericFieldValueTypes() {
        List<String> numericTypes = List.of("long", "int", "short", "byte", "double", "float");
        for (String numericType : numericTypes) {
            assertThat(fieldTypeValidation.validateFieldValueType(term("123"), numericType)).isNotPresent();
            assertThat(fieldTypeValidation.validateFieldValueType(term(">123"), numericType)).isNotPresent();
            assertThat(fieldTypeValidation.validateFieldValueType(term(">=123"), numericType)).isNotPresent();
            assertThat(fieldTypeValidation.validateFieldValueType(term("<42"), numericType)).isNotPresent();
            assertThat(fieldTypeValidation.validateFieldValueType(term("<=42"), numericType)).isNotPresent();

            assertThat(fieldTypeValidation.validateFieldValueType(term("ABC"), numericType)).isPresent();
        }
    }

    @Test
    void validateDateFieldValueType() {
        assertThat(fieldTypeValidation.validateFieldValueType(term("2019-07-23 09:53:08.175"), "date")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("2019-07-23 09:53:08"), "date")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("2019-07-23"), "date")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("2020-07-29T12:00:00.000-05:00"), "date")).isNotPresent();

        assertThat(fieldTypeValidation.validateFieldValueType(term("ABC"), "date")).isPresent();
    }

    @Test
    void validateIPFieldValueType() {
        // IPv4
        assertThat(fieldTypeValidation.validateFieldValueType(term("123.34.45.56"), "ip")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("123.999.45.56"), "ip")).isPresent();

        //IPv6
        assertThat(fieldTypeValidation.validateFieldValueType(term("2001:db8:3333:4444:5555:6666:7777:8888"), "ip")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("2001:db8:3333:4444:CCCC:DDDD:EEEE:FFFF"), "ip")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("::"), "ip")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("2001:db8::"), "ip")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("::1234:5678"), "ip")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("2001:db8::1234:5678"), "ip")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("2001:0db8:0001:0000:0000:0ab9:C0A8:0102"), "ip")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("2001:db8:1::ab9:C0A8:102"), "ip")).isNotPresent();

        // CIDR
        assertThat(fieldTypeValidation.validateFieldValueType(term("123.34.45.56/24"), "ip")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("123.34.45.56/"), "ip")).isPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("123.34.45.56/299"), "ip")).isPresent();

        // just wrong
        assertThat(fieldTypeValidation.validateFieldValueType(term("ABC"), "ip")).isPresent();
    }

    @Test
    void validateBooleanFieldValueType() {
        assertThat(fieldTypeValidation.validateFieldValueType(term("true"), "boolean")).isNotPresent();
        assertThat(fieldTypeValidation.validateFieldValueType(term("false"), "boolean")).isNotPresent();

        assertThat(fieldTypeValidation.validateFieldValueType(term("hard to say"), "boolean")).isNotPresent();
    }

    @Test
    void testDateMathExpressions() {
        isValidTerm("now", "date");
        isValidTerm("now+4d", "date");
        isValidTerm("now+24m", "date");
        isValidTerm("now+1h+1m", "date");

        isValidTerm("2019-07-2||+1m", "date");
        isValidTerm("2019-07-23 09:53:08.175||+1d", "date");
        isValidTerm("2014-11-18T14:27:32||-3600s", "date");
        isValidTerm("2014-11-18||+1M/M+1h", "date");

        isNotValidTerm("now+1h+nonsence", "date");
        isNotValidTerm("2019-07-2||", "date");
    }

    private void isValidTerm(String term, String fieldType) {
        assertThat(fieldTypeValidation.validateFieldValueType(term(term),fieldType))
                .isNotPresent();
    }

    private void isNotValidTerm(String term, String fieldType) {
        assertThat(fieldTypeValidation.validateFieldValueType(term(term),fieldType))
                .isPresent();
    }

    private ParsedTerm term(String value) {
        final Token token = Token.newToken(QueryParserConstants.TERM, "foo");
        token.beginLine = 1;
        token.beginColumn = 0;
        token.endLine = 1;
        token.endColumn = 3;
        return ParsedTerm.builder()
                .field("foo")
                .valueToken(ImmutableToken.create(token))
                .value(value)
                .build();
    }
}
