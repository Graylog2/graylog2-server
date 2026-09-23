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
package org.graylog2.indexer.fieldtypes;

import org.graylog.plugins.views.search.searchtypes.events.CommonEventSummary;
import org.graylog2.plugin.streams.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.copyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.fieldtypes.FieldTypes.Type.createType;

public class FieldTypeMapperTest {
    private FieldTypeMapper mapper;
    private static final FieldTypeDTO textWithFielddata = FieldTypeDTO.builder()
            .physicalType("text")
            .fieldName("test")
            .properties(Collections.singleton(FieldTypeDTO.Properties.FIELDDATA))
            .build();

    @BeforeEach
    public void setUp() throws Exception {
        this.mapper = new FieldTypeMapper();
    }

    private void assertMapping(String esType, String glType, String... properties) {
        assertMapping(FieldTypeDTO.builder()
                        .fieldName("test")
                        .physicalType(esType)
                        .build(),
                glType,
                properties);
    }

    private void assertMapping(FieldTypeDTO esType, String glType, String... properties) {
        assertMapping(esType, Set.of(), glType, properties);
    }

    private void assertMapping(FieldTypeDTO esType, Set<String> queryStreamIds, String glType, String... properties) {
        assertThat(mapper.mapType(esType, queryStreamIds))
                .isPresent().get()
                .isEqualTo(createType(glType, copyOf(properties)));
    }

    @Test
    public void mappings() {
        assertMapping("text", "string", "full-text-search");
        assertMapping(textWithFielddata, "string", "full-text-search", "enumerable");
        assertMapping("keyword", "string", "enumerable");

        assertMapping("long", "long", "numeric", "enumerable");
        assertMapping("integer", "int", "numeric", "enumerable");
        assertMapping("short", "short", "numeric", "enumerable");
        assertMapping("byte", "byte", "numeric", "enumerable");
        assertMapping("double", "double", "numeric", "enumerable");
        assertMapping("float", "float", "numeric", "enumerable");
        assertMapping("half_float", "float", "numeric", "enumerable");
        assertMapping("scaled_float", "float", "numeric", "enumerable");

        assertMapping("date", "date", "enumerable");
        assertMapping("boolean", "boolean", "enumerable");
        assertMapping("binary", "binary");
        assertMapping("geo_point", "geo-point");
        assertMapping("ip", "ip", "enumerable");
    }

    @Test
    public void identifiesNumericPhysicalTypes() {
        assertThat(FieldTypeMapper.isNumericType("long")).isTrue();
        assertThat(FieldTypeMapper.isNumericType("integer")).isTrue();
        assertThat(FieldTypeMapper.isNumericType("short")).isTrue();
        assertThat(FieldTypeMapper.isNumericType("byte")).isTrue();
        assertThat(FieldTypeMapper.isNumericType("double")).isTrue();
        assertThat(FieldTypeMapper.isNumericType("float")).isTrue();
        assertThat(FieldTypeMapper.isNumericType("half_float")).isTrue();
        assertThat(FieldTypeMapper.isNumericType("scaled_float")).isTrue();

        assertThat(FieldTypeMapper.isNumericType("keyword")).isFalse();
        assertThat(FieldTypeMapper.isNumericType("text")).isFalse();
        assertThat(FieldTypeMapper.isNumericType("date")).isFalse();
        assertThat(FieldTypeMapper.isNumericType("boolean")).isFalse();
        assertThat(FieldTypeMapper.isNumericType("ip")).isFalse();
        assertThat(FieldTypeMapper.isNumericType("unknown_type")).isFalse();
    }

    @Test
    public void mapsPriorityFieldOnlyWhenQueryIsConfinedToEventStreams() {
        final FieldTypeDTO priorityField = FieldTypeDTO.builder()
                .fieldName(CommonEventSummary.FIELD_PRIORITY)
                .physicalType("long")
                .build();

        assertMapping(priorityField, Set.of(Stream.DEFAULT_EVENTS_STREAM_ID, Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID),
                "priority", "numeric", "enumerable");
        assertMapping(priorityField, Set.of(Stream.DEFAULT_EVENTS_STREAM_ID), "priority", "numeric", "enumerable");

        assertMapping(priorityField, Set.of(Stream.DEFAULT_EVENTS_STREAM_ID, "5f4dfb144b8ea2d1819e2e2e"),
                "long", "numeric", "enumerable");
        assertMapping(priorityField, Set.of("5f4dfb144b8ea2d1819e2e2e"), "long", "numeric", "enumerable");
        assertMapping(priorityField, Set.of(), "long", "numeric", "enumerable");
    }

    @Test
    public void mapsAlertFieldOnlyWhenQueryIsConfinedToEventStreams() {
        final FieldTypeDTO alertField = FieldTypeDTO.builder()
                .fieldName(CommonEventSummary.FIELD_ALERT)
                .physicalType("boolean")
                .build();

        assertMapping(alertField, Set.of(Stream.DEFAULT_EVENTS_STREAM_ID, Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID),
                "alert", "enumerable");
        assertMapping(alertField, Set.of(Stream.DEFAULT_EVENTS_STREAM_ID), "alert", "enumerable");

        assertMapping(alertField, Set.of(Stream.DEFAULT_EVENTS_STREAM_ID, "5f4dfb144b8ea2d1819e2e2e"),
                "boolean", "enumerable");
        assertMapping(alertField, Set.of("5f4dfb144b8ea2d1819e2e2e"), "boolean", "enumerable");
        assertMapping(alertField, Set.of(), "boolean", "enumerable");
    }
}
