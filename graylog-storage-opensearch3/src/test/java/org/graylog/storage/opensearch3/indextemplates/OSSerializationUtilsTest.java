package org.graylog.storage.opensearch3.indextemplates;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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


    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    OSSerializationUtils toTest;

    @BeforeEach
    void setUp() {
        toTest = new OSSerializationUtils(objectMapper);
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

}
