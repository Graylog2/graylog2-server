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
package org.graylog.storage.opensearch3.indextemplates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.mapping.DynamicTemplate;
import org.opensearch.client.opensearch._types.mapping.KeywordProperty;
import org.opensearch.client.opensearch._types.mapping.LongNumberProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.SourceField;
import org.opensearch.client.opensearch._types.mapping.TextProperty;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OSSerializationUtilsTest {

    private static final TypeMapping TEST_TYPE_MAPPING = TypeMapping.builder()
            .dynamicTemplates(
                    List.of(
                            Map.of("internal_fields", DynamicTemplate.builder()
                                    .match("gl2_*")
                                    .matchMappingType("string")
                                    .mapping(Property.builder().keyword(KeywordProperty.builder().build()).build())
                                    .build()),
                            Map.of("store_generic", DynamicTemplate.builder()
                                    .matchMappingType("string")
                                    .mapping(Property.builder().keyword(KeywordProperty.builder().build()).build())
                                    .build())
                    )
            )
            .properties(
                    Map.of(
                            "message",
                            Property.builder()
                                    .text(TextProperty.builder().analyzer("standard").fielddata(false).build())
                                    .build(),
                            "http_response_code",
                            Property.builder()
                                    .long_(LongNumberProperty.builder().build())
                                    .build()
                    )
            )
            .source(
                    SourceField.builder()
                            .enabled(true)
                            .build()
            )
            .build();

    public static final Map<String, Object> TEST_TYPE_MAPPING_IN_MAP_FORMAT = Map.of(
            "dynamic_templates",
            List.of(
                    Map.of(
                            "internal_fields",
                            Map.of(
                                    "match", "gl2_*",
                                    "match_mapping_type", "string",
                                    "mapping", Map.of("type", "keyword")
                            )
                    ),
                    Map.of(
                            "store_generic",
                            Map.of(
                                    "match_mapping_type", "string",
                                    "mapping", Map.of("type", "keyword")
                            )
                    )
            ),
            "properties",
            Map.of(
                    "message",
                    Map.of("type", "text", "analyzer", "standard", "fielddata", false),
                    "http_response_code",
                    Map.of("type", "long")
            ),
            "_source",
            Map.of("enabled", true));


    private OSSerializationUtils toTest;

    @BeforeEach
    void setUp() {
        toTest = new OSSerializationUtils();
    }

    @Test
    void testFromMapOnTypeMappingClass() throws Exception {
        final TypeMapping result = toTest.fromMap(
                TEST_TYPE_MAPPING_IN_MAP_FORMAT,
                TypeMapping._DESERIALIZER);
        assertEquals(TEST_TYPE_MAPPING, result);
    }

    @Test
    void testToMapOnTypeMappingClass() throws Exception {
        final Map<String, Object> result = toTest.toMap(TEST_TYPE_MAPPING);
        assertEquals(TEST_TYPE_MAPPING_IN_MAP_FORMAT, result);
    }

    @Test
    void testFromStringOnTypeMappingClass() {
        final String metaKey = "meta";
        final ErrorResponse expected = new ErrorResponse.Builder()
                .status(404)
                .error(ErrorCause.builder()
                        .type("index_not_found")
                        .reason("Index not found")
                        .metadata(Map.of(metaKey, JsonData.of(123)))
                        .build())
                .build();
        final String json = expected.toJsonString();
        final ErrorResponse errorResponse = toTest.fromJson(json, ErrorResponse._DESERIALIZER);
        assertEquals(expected.status(), errorResponse.status());
        assertEquals(expected.error().type(), errorResponse.error().type());
        assertEquals(expected.error().reason(), errorResponse.error().reason());
        assertEquals(expected.error().metadata().get(metaKey).to(Integer.class), errorResponse.error().metadata().get(metaKey).to(Integer.class));
    }

    @Test
    void testToJsonMap() {
        assertEquals(Map.of(), toTest.toJsonDataMap(Map.of()));
        final Map<String, JsonData> converted = toTest.toJsonDataMap(Map.of("the_number", 42));

        //JsonDataImpl does not implement equals(), so verification is slightly awkward
        assertEquals(1, converted.size());
        final JsonData toVerify = converted.get("the_number");
        assertEquals(42, toVerify.to(Integer.class));
    }
}
