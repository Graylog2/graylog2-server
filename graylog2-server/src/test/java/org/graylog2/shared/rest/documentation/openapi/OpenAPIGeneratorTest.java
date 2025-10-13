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
package org.graylog2.shared.rest.documentation.openapi;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog.grn.GRNRegistry;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.Version;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.bindings.providers.config.ObjectMapperConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAPIGeneratorTest {

    private OpenAPIGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new OpenAPIGenerator(
                Set.of(TestResource.class),
                Collections.emptyMap(),
                Version.from(1, 0, 0),
                new ObjectMapperConfiguration(
                        ObjectMapperProvider.class.getClassLoader(),
                        Collections.emptySet(),
                        new EncryptedValueService(UUID.randomUUID().toString()),
                        GRNRegistry.createWithBuiltinTypes(),
                        InputConfigurationBeanDeserializerModifier.withoutConfig()
                )
        );
    }

    @Test
    void generateOpenApiSpec_createsValidOpenAPIObject() {
        final var openAPI = generator.generateOpenApiSpec();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Graylog REST API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getInfo().getDescription()).contains("REST API");
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("Graylog");
        assertThat(openAPI.getInfo().getLicense()).isNotNull();
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("SSPLv1");

        // Verify that paths and schemas are generated
        assertThat(openAPI.getPaths()).isNotEmpty();
        assertThat(openAPI.getComponents().getSchemas()).isNotEmpty();
    }

    @Test
    void canGenerateSchemaForResourceWithOptionalPrimitives() {
        // This test verifies that the OpenAPI generation correctly handles OptionalInt/OptionalLong/OptionalDouble
        // fields by treating them the same as Optional<Integer>/Optional<Long>/Optional<Double>.
        //
        // See: https://github.com/swagger-api/swagger-core/issues/4717

        final var generatedOpenAPI = generator.generateOpenApiSpec();

        assertThat(generatedOpenAPI).isNotNull();
        assertThat(generatedOpenAPI.getPaths()).isNotEmpty();

        // Verify that the /test path exists
        assertThat(generatedOpenAPI.getPaths()).containsKey("/test");

        // Print the generated spec for debugging
        final var spec = Json.pretty(generatedOpenAPI);
        System.out.println("Generated OpenAPI spec:");
        System.out.println(spec);

        // Verify the schema contains our test endpoint
        final var postOperation = generatedOpenAPI.getPaths().get("/test").getPost();
        assertThat(postOperation).isNotNull();
        assertThat(postOperation.getOperationId()).isEqualTo("createTest");

        // Verify that OptionalInt/OptionalLong/OptionalDouble are treated the same as Optional<Integer>/Optional<Long>/Optional<Double>
        final var responseSchema = generatedOpenAPI.getComponents().getSchemas().get("TestResponse");
        assertThat(responseSchema).isNotNull();

        // The ObjectMapper converts camelCase to snake_case
        final var optionalIntegerProperty = (Schema<?>) responseSchema.getProperties().get("optional_integer");
        final var countProperty = (Schema<?>) responseSchema.getProperties().get("count");
        final var optionalLongProperty = (Schema<?>) responseSchema.getProperties().get("optional_long");
        final var limitProperty = (Schema<?>) responseSchema.getProperties().get("limit");
        final var optionalDoubleProperty = (Schema<?>) responseSchema.getProperties().get("optional_double");
        final var rateProperty = (Schema<?>) responseSchema.getProperties().get("rate");

        // Optional<Integer> and OptionalInt should both be simple integer schemas with int32 format
        assertThat(optionalIntegerProperty.getType()).isEqualTo("integer");
        assertThat(optionalIntegerProperty.getFormat()).isEqualTo("int32");
        assertThat(countProperty.getType()).isEqualTo("integer");
        assertThat(countProperty.getFormat()).isEqualTo("int32");

        // Optional<Long> and OptionalLong should both be simple integer schemas with int64 format
        assertThat(optionalLongProperty.getType()).isEqualTo("integer");
        assertThat(optionalLongProperty.getFormat()).isEqualTo("int64");
        assertThat(limitProperty.getType()).isEqualTo("integer");
        assertThat(limitProperty.getFormat()).isEqualTo("int64");

        // Optional<Double> and OptionalDouble should both be simple number schemas with double format
        assertThat(optionalDoubleProperty.getType()).isEqualTo("number");
        assertThat(optionalDoubleProperty.getFormat()).isEqualTo("double");
        assertThat(rateProperty.getType()).isEqualTo("number");
        assertThat(rateProperty.getFormat()).isEqualTo("double");

        // None should be objects with {empty, present, asInt/asLong/asDouble} properties
        assertThat(countProperty.getProperties()).isNullOrEmpty();
        assertThat(limitProperty.getProperties()).isNullOrEmpty();
        assertThat(rateProperty.getProperties()).isNullOrEmpty();
    }

    // Test resource with primitive Optional types to verify schema generation
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class TestResource {

        @NoAuditEvent("Test")
        @GET
        public TestResponse getTest() {
            return new TestResponse(
                    "test",
                    Optional.of("value"),
                    Optional.of(123),
                    OptionalInt.of(42),
                    Optional.of(999L),
                    OptionalLong.of(1000L),
                    Optional.of(3.14),
                    OptionalDouble.of(2.71)
            );
        }

        @NoAuditEvent("Test")
        @POST
        public TestResponse createTest(TestRequest request) {
            return new TestResponse(
                    request.name(),
                    request.description(),
                    request.optionalInteger(),
                    request.count(),
                    request.optionalLong(),
                    request.limit(),
                    request.optionalDouble(),
                    request.rate()
            );
        }
    }

    public record TestRequest(
            String name,
            Optional<String> description,
            Optional<Integer> optionalInteger,  // For comparison with OptionalInt
            OptionalInt count,
            Optional<Long> optionalLong,  // For comparison with OptionalLong
            OptionalLong limit,
            Optional<Double> optionalDouble,  // For comparison with OptionalDouble
            OptionalDouble rate
    ) {}

    public record TestResponse(
            String name,
            Optional<String> description,
            Optional<Integer> optionalInteger,  // For comparison with OptionalInt
            OptionalInt count,
            Optional<Long> optionalLong,  // For comparison with OptionalLong
            OptionalLong limit,
            Optional<Double> optionalDouble,  // For comparison with OptionalDouble
            OptionalDouble rate
    ) {}
}
