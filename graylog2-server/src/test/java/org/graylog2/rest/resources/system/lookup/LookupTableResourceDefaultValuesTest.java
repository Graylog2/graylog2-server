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
package org.graylog2.rest.resources.system.lookup;

import jakarta.ws.rs.BadRequestException;
import org.graylog2.lookup.LookupDefaultSingleValue;
import org.graylog2.rest.models.system.lookup.LookupTableApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class LookupTableResourceDefaultValuesTest {

    private static LookupTableApi table(String singleValue, LookupDefaultSingleValue.Type singleType,
                                        String multiValue, LookupDefaultSingleValue.Type multiType) {
        return LookupTableApi.builder()
                .title("Title")
                .description("")
                .name("name")
                .cacheId("cache-id")
                .dataAdapterId("adapter-id")
                .defaultSingleValue(singleValue)
                .defaultSingleValueType(singleType)
                .defaultMultiValue(multiValue)
                .defaultMultiValueType(multiType)
                .build();
    }

    @Test
    void trimsDefaultValues() {
        final LookupTableApi trimmed = LookupTableResource.trimDefaultValues(
                table(" true ", LookupDefaultSingleValue.Type.BOOLEAN, " {\"a\": 1} ", LookupDefaultSingleValue.Type.OBJECT));

        assertThat(trimmed.defaultSingleValue()).isEqualTo("true");
        assertThat(trimmed.defaultMultiValue()).isEqualTo("{\"a\": 1}");
        assertThat(trimmed.name()).isEqualTo("name");
    }

    @Test
    void acceptsValidDefaultValues() {
        final LookupTableApi lookupTable = table("unknown", LookupDefaultSingleValue.Type.STRING, "{\"a\": 1}", LookupDefaultSingleValue.Type.OBJECT);

        assertThat(LookupTableResource.validateDefaultValues(lookupTable).failed()).isFalse();
        assertThat(LookupTableResource.checkDefaultValues(lookupTable)).isSameAs(lookupTable);
    }

    @Test
    void acceptsUnsetDefaultValues() {
        assertThat(LookupTableResource.validateDefaultValues(
                table("", LookupDefaultSingleValue.Type.NULL, "", LookupDefaultSingleValue.Type.NULL)).failed()).isFalse();
    }

    @Test
    void rejectsUnparseableMultiValue() {
        final LookupTableApi lookupTable = table("", LookupDefaultSingleValue.Type.NULL, "hello", LookupDefaultSingleValue.Type.OBJECT);

        assertThat(LookupTableResource.validateDefaultValues(lookupTable).getErrors())
                .containsOnlyKeys(LookupTableApi.FIELD_DEFAULT_MULTI_VALUE);
        assertThatThrownBy(() -> LookupTableResource.checkDefaultValues(lookupTable))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsUnparseableNumber() {
        assertThat(LookupTableResource.validateDefaultValues(
                table("abc", LookupDefaultSingleValue.Type.NUMBER, "", LookupDefaultSingleValue.Type.NULL)).getErrors())
                .containsExactly(entry(LookupTableApi.FIELD_DEFAULT_SINGLE_VALUE, List.of("Must be a number.")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes", "1", "ture"})
    void rejectsNonBooleanValue(String value) {
        assertThat(LookupTableResource.validateDefaultValues(
                table(value, LookupDefaultSingleValue.Type.BOOLEAN, "", LookupDefaultSingleValue.Type.NULL)).getErrors())
                .containsExactly(entry(LookupTableApi.FIELD_DEFAULT_SINGLE_VALUE, List.of("Must be true or false.")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "FALSE", "True"})
    void acceptsBooleanValueInAnyCase(String value) {
        assertThat(LookupTableResource.validateDefaultValues(
                table(value, LookupDefaultSingleValue.Type.BOOLEAN, "", LookupDefaultSingleValue.Type.NULL)).failed()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = LookupDefaultSingleValue.Type.class, names = {"NUMBER", "BOOLEAN"})
    void rejectsEmptyNonStringSingleValue(LookupDefaultSingleValue.Type type) {
        assertThat(LookupTableResource.validateDefaultValues(
                table("", type, "", LookupDefaultSingleValue.Type.NULL)).getErrors())
                .containsOnlyKeys(LookupTableApi.FIELD_DEFAULT_SINGLE_VALUE);
    }

    @Test
    void rejectsEmptyStringOnceTrimmed() {
        final LookupTableApi lookupTable = LookupTableResource.trimDefaultValues(
                table("   ", LookupDefaultSingleValue.Type.STRING, "", LookupDefaultSingleValue.Type.NULL));

        assertThatThrownBy(() -> LookupTableResource.checkDefaultValues(lookupTable))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("default_single_value: Must not be empty.");
    }
}
