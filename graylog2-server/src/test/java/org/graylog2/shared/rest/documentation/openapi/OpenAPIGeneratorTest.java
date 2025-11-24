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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.graylog.grn.GRNRegistry;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.bindings.providers.config.ObjectMapperConfiguration;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAPIGeneratorTest {

    private OpenAPIGenerator generator;
    private ObjectMapperConfiguration objectMapperConfiguration;

    @BeforeEach
    void setUp() {
        final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources =
                Map.of("my.plugin.id", Set.of(TestResource.class, TestResource2.class, NotPublicCloudResource.class));

        objectMapperConfiguration = new ObjectMapperConfiguration(
                ObjectMapperProvider.class.getClassLoader(),
                Set.of(new NamedType(ChildType1.class, "child-type-1"), new NamedType(ChildType2.class, "child-type-2")),
                new EncryptedValueService(UUID.randomUUID().toString()),
                GRNRegistry.createWithBuiltinTypes(),
                InputConfigurationBeanDeserializerModifier.withoutConfig()
        );

        generator = new OpenAPIGenerator(
                Version.from(1, 0, 0),
                new CustomOpenAPIScanner(Set.of(RootTestResource.class), pluginRestResources, true),
                new CustomObjectMapperProcessor(objectMapperConfiguration),
                new CustomModelConverter(objectMapperConfiguration.configure(new ObjectMapper())),
                (config) -> new CustomReader(pluginRestResources, config)
        );
    }

    @Test
    void createsValidOpenAPIObject() {
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
        assertThat(openAPI.getPaths()).containsOnlyKeys("/test", "/plugins/my.plugin.id/test", "/plugins/my.plugin.id/subtypes");
        assertThat(openAPI.getComponents().getSchemas()).isNotEmpty();
    }

    @Test
    void canGenerateSchemaForResourceWithOptionalPrimitives() {
        // This test verifies that the OpenAPI generation correctly handles OptionalInt/OptionalLong/OptionalDouble
        // fields by treating them the same as Optional<Integer>/Optional<Long>/Optional<Double>.
        //
        // See: https://github.com/swagger-api/swagger-core/issues/4717
        final var generatedOpenAPI = generator.generateOpenApiSpec();
        try {
            assertThat(generatedOpenAPI).isNotNull();
            assertThat(generatedOpenAPI.getPaths()).isNotEmpty();

            // Verify that the /test path exists
            assertThat(generatedOpenAPI.getPaths()).containsKey("/plugins/my.plugin.id/test");

            // Verify the schema contains our test endpoint
            final var postOperation = generatedOpenAPI.getPaths().get("/plugins/my.plugin.id/test").getPost();
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
            assertThat(optionalIntegerProperty.getTypes()).containsExactly("integer");
            assertThat(optionalIntegerProperty.getFormat()).isEqualTo("int32");
            assertThat(countProperty.getTypes()).containsExactly("integer");
            assertThat(countProperty.getFormat()).isEqualTo("int32");

            // Optional<Long> and OptionalLong should both be simple integer schemas with int64 format
            assertThat(optionalLongProperty.getTypes()).containsExactly("integer");
            assertThat(optionalLongProperty.getFormat()).isEqualTo("int64");
            assertThat(limitProperty.getTypes()).containsExactly("integer");
            assertThat(limitProperty.getFormat()).isEqualTo("int64");

            // Optional<Double> and OptionalDouble should both be simple number schemas with double format
            assertThat(optionalDoubleProperty.getTypes()).containsExactly("number");
            assertThat(optionalDoubleProperty.getFormat()).isEqualTo("double");
            assertThat(rateProperty.getTypes()).containsExactly("number");
            assertThat(rateProperty.getFormat()).isEqualTo("double");

            // None should be objects with {empty, present, asInt/asLong/asDouble} properties
            assertThat(countProperty.getProperties()).isNullOrEmpty();
            assertThat(limitProperty.getProperties()).isNullOrEmpty();
            assertThat(rateProperty.getProperties()).isNullOrEmpty();
        } catch (AssertionError | NullPointerException e) {
            System.err.println("Test failed. Full OpenAPI spec:");
            System.err.println(Json31.pretty(generatedOpenAPI));
            throw e;
        }
    }

    @Test
    void handlesJacksonSubtypes() throws Exception {
        final var mapper = objectMapperConfiguration.configure(new ObjectMapper());

        final var childType1 = mapper.readValue("{\"type\":\"child-type-1\",\"age\":5}", ParentType.class);
        final var childType2 = mapper.readValue("{\"type\":\"child-type-2\",\"age\":5}", ParentType.class);

        assertThat(childType1).isInstanceOf(ChildType1.class);
        assertThat(childType2).isInstanceOf(ChildType2.class);

        final var openAPI = generator.generateOpenApiSpec();

        try {
            // Verify discriminator mapping uses type aliases, not FQ class names
            final var parentTypeSchema = openAPI.getComponents().getSchemas().get("ParentType");
            final var discriminatorMapping = parentTypeSchema.getDiscriminator().getMapping();
            assertThat(discriminatorMapping)
                    .containsEntry("child-type-1", "#/components/schemas/ChildType1")
                    .containsEntry("child-type-2", "#/components/schemas/ChildType2");

            // Verify ChildType1 has allOf with ParentType reference
            final Schema<?> childType1Schema = openAPI.getComponents().getSchemas().get("ChildType1");
            final var childType1HasParentRef = childType1Schema.getAllOf().stream()
                    .map(s -> (Schema<?>) s)
                    .anyMatch(schema -> "#/components/schemas/ParentType".equals(schema.get$ref()));
            assertThat(childType1HasParentRef).isTrue();

            // Verify ChildType2 has allOf with ParentType reference
            final Schema<?> childType2Schema = openAPI.getComponents().getSchemas().get("ChildType2");
            final var childType2HasParentRef = childType2Schema.getAllOf().stream()
                    .map(s -> (Schema<?>) s)
                    .anyMatch(schema -> "#/components/schemas/ParentType".equals(schema.get$ref()));
            assertThat(childType2HasParentRef).isTrue();

        } catch (AssertionError | NullPointerException e) {
            System.err.println("Test failed. Full OpenAPI spec:");
            System.err.println(Json31.pretty(openAPI));
            throw e;
        }
    }

    @PublicCloudAPI
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class RootTestResource {

        @NoAuditEvent("RootTest")
        @GET
        public Response getTest() {
            return Response.ok().build();
        }
    }

    // Test resource with primitive Optional types to verify schema generation
    @PublicCloudAPI
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class TestResource implements PluginRestResource {

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

    @PublicCloudAPI
    @Path("/subtypes")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class TestResource2 implements PluginRestResource {

        @NoAuditEvent("Test")
        @GET
        public ParentType getTest() {
            if (Math.random() < 0.5) {
                return new ChildType1(2);
            } else {
                return new ChildType2(2);
            }
        }
    }

    @Path("/not-public-cloud-api")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class NotPublicCloudResource implements PluginRestResource {

        @NoAuditEvent("Test")
        @GET
        public Response getTest() {
            return Response.ok().build();
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

    // TODO: at the moment, subtypes are only correctly handled when they are statically annotated with @JsonSubTypes
    //   We need this to work with subtypes that come from plugins and are registered with the ObjectMapper at runtime,
    //   but this will require changes to the model resolver.
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ChildType1.class, name = "child-type-1"),
            @JsonSubTypes.Type(value = ChildType2.class, name = "child-type-2"),
    })
    public interface ParentType {
        String type();
    }

    @JsonTypeName("child-type-1")
    public record ChildType1(int age) implements ParentType {
        @Override
        @JsonProperty("type")
        public String type() {
            return "child-type-1";
        }
    }

    @JsonTypeName("child-type-2")
    public record ChildType2(long age) implements ParentType {
        @Override
        @JsonProperty("type")
        public String type() {
            return "child-type-2";
        }
    }
}
