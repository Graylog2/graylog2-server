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

import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.Message.RESERVED_SETTABLE_FIELDS;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UnknownFieldsValidatorTest {

    private UnknownFieldsValidator toTest;

    @BeforeEach
    void setUp() {
        toTest = new UnknownFieldsValidator();
    }

    @Test
    void testDoesNotIdentifySpecialIndexFieldAsUnknown() {
        final List<ParsedTerm> unknownFields = toTest.identifyUnknownFields(
                Set.of("some_normal_field"),
                List.of(ParsedTerm.create("_index", "graylog_0"))
        );
        assertTrue(unknownFields.isEmpty());
    }

    @Test
    void testDoesNotIdentifySpecialIdFieldAsUnknown() {
        final List<ParsedTerm> unknownFields = toTest.identifyUnknownFields(
                Set.of("some_normal_field"),
                List.of(ParsedTerm.create("_id", "buba"))
        );
        assertTrue(unknownFields.isEmpty());
    }

    @Test
    void testDoesNotIdentifyGraylogReservedFieldsAsUnknown() {
        final List<ParsedTerm> unknownFields = toTest.identifyUnknownFields(
                Set.of("some_normal_field"),
                RESERVED_SETTABLE_FIELDS.stream().map(f -> ParsedTerm.create(f, "buba")).collect(Collectors.toList())
        );
        assertTrue(unknownFields.isEmpty());
    }

    @Test
    void testDoesNotIdentifyDefaultFieldAsUnknown() {
        final List<ParsedTerm> unknownFields = toTest.identifyUnknownFields(
                Set.of("some_normal_field"),
                List.of(ParsedTerm.create(ParsedTerm.DEFAULT_FIELD, "Haba, haba, haba!"))
        );
        assertTrue(unknownFields.isEmpty());
    }

    @Test
    void testDoesNotIdentifyAvailableFieldAsUnknown() {
        final List<ParsedTerm> unknownFields = toTest.identifyUnknownFields(
                Set.of("some_normal_field"),
                List.of(ParsedTerm.create("some_normal_field", "Haba, haba, haba!"))
        );
        assertTrue(unknownFields.isEmpty());
    }

    @Test
    void testIdentifiesUnknownField() {
        final ParsedTerm unknownField = ParsedTerm.create("strange_field", "!!!");
        final List<ParsedTerm> unknownFields = toTest.identifyUnknownFields(
                Set.of("some_normal_field"),
                List.of(
                        ParsedTerm.create("some_normal_field", "Haba, haba, haba!"),
                        unknownField

                )
        );
        assertThat(unknownFields)
                .hasSize(1)
                .contains(unknownField);
    }
}
