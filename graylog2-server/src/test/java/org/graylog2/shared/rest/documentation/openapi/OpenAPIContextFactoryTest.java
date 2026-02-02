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
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class OpenAPIContextFactoryTest {

    private static ObjectMapper objectMapper;
    private static OpenAPI generatedDescription;

    @BeforeAll
    static void beforeAll() {
        final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources =
                Map.of("my.plugin.id", Set.of(TestResource.class, TestResource2.class,
                        NotPublicCloudResource.class, SchemaConflictResource.class, ImmutableMapsResource.class));

        objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(
                new NamedType(ChildType3.class, "child-type-3"),
                new NamedType(ChildType4.class, "child-type-4")
        );

        final var openAPIContextFactory = new OpenAPIContextFactory(
                Version.from(1, 0, 0),
                new CustomOpenAPIScanner(Set.of(RootTestResource.class), pluginRestResources, true),
                new CustomModelConverter(objectMapper),
                (config) -> new CustomReader(pluginRestResources, config)
        );

        generatedDescription = openAPIContextFactory.getOrCreate("test-context").read();
    }

    @Test
    void createsValidOpenAPIObject() {
        try {
            assertThat(generatedDescription).isNotNull();
            assertThat(generatedDescription.getInfo()).isNotNull();
            assertThat(generatedDescription.getInfo().getTitle()).isEqualTo("REST API");
            assertThat(generatedDescription.getInfo().getVersion()).isEqualTo("1.0.0");
            assertThat(generatedDescription.getInfo().getDescription()).contains("REST API");
            assertThat(generatedDescription.getInfo().getLicense()).isNotNull();
            assertThat(generatedDescription.getInfo().getLicense().getName()).isEqualTo("SSPLv1");

            // Verify that paths and schemas are generated
            assertThat(generatedDescription.getPaths()).containsOnlyKeys("/test", "/plugins/my.plugin.id/test",
                    "/plugins/my.plugin.id/subtypes", "/plugins/my.plugin.id/response-schema-name-conflict/pkg1",
                    "/plugins/my.plugin.id/response-schema-name-conflict/pkg2", "/plugins/my.plugin.id/immutable-maps");
            assertThat(generatedDescription.getComponents().getSchemas()).isNotEmpty();
        } catch (AssertionError | NullPointerException e) {
            System.err.println("Test failed. Full OpenAPI description:");
            System.err.println(Json31.pretty(generatedDescription));
            throw e;
        }
    }

    @Test
    void canGenerateSchemaForResourceWithOptionalPrimitives() {
        // This test verifies that the OpenAPI generation correctly handles OptionalInt/OptionalLong/OptionalDouble
        // fields by treating them the same as Optional<Integer>/Optional<Long>/Optional<Double>.
        //
        // See: https://github.com/swagger-api/swagger-core/issues/4717
        try {
            assertThat(generatedDescription).isNotNull();
            assertThat(generatedDescription.getPaths()).isNotEmpty();

            // Verify that the /test path exists
            assertThat(generatedDescription.getPaths()).containsKey("/plugins/my.plugin.id/test");

            // Verify the schema contains our test endpoint
            final var postOperation = generatedDescription.getPaths().get("/plugins/my.plugin.id/test").getPost();
            assertThat(postOperation).isNotNull();
            assertThat(postOperation.getOperationId()).isEqualTo("createTest");

            // Verify that OptionalInt/OptionalLong/OptionalDouble are treated the same as Optional<Integer>/Optional<Long>/Optional<Double>
            final var responseSchema = generatedDescription.getComponents().getSchemas()
                    .get("org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.TestResponse");
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
            System.err.println("Test failed. Full OpenAPI description:");
            System.err.println(Json31.pretty(generatedDescription));
            throw e;
        }
    }

    @Test
    void handlesJacksonSubtypes() throws Exception {

        final var childType1 = objectMapper.readValue("{\"type\":\"child-type-1\",\"age\":5}", ParentType.class);
        final var childType2 = objectMapper.readValue("{\"type\":\"child-type-2\",\"age\":5}", ParentType.class);

        assertThat(childType1).isInstanceOf(ChildType1.class);
        assertThat(childType2).isInstanceOf(ChildType2.class);

        try {
            // Verify ChildType1 has allOf with ParentType reference
            final Schema<?> childType1Schema = generatedDescription.getComponents().getSchemas()
                    .get("org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ChildType1");
            final var childType1HasParentRef = childType1Schema.getAllOf().stream()
                    .map(s -> (Schema<?>) s)
                    .anyMatch(schema ->
                            "#/components/schemas/org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ParentType".equals(schema.get$ref()));
            assertThat(childType1HasParentRef).isTrue();

            // Verify ChildType2 has allOf with ParentType reference
            final Schema<?> childType2Schema = generatedDescription.getComponents().getSchemas()
                    .get("org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ChildType2");
            final var childType2HasParentRef = childType2Schema.getAllOf().stream()
                    .map(s -> (Schema<?>) s)
                    .anyMatch(schema ->
                            "#/components/schemas/org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ParentType".equals(schema.get$ref()));
            assertThat(childType2HasParentRef).isTrue();

            // Verify ChildType3 has allOf with ParentType reference
            final Schema<?> childType3Schema = generatedDescription.getComponents().getSchemas()
                    .get("org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ChildType3");
            final var childType3HasParentRef = childType3Schema.getAllOf().stream()
                    .map(s -> (Schema<?>) s)
                    .anyMatch(schema ->
                            "#/components/schemas/org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ParentType".equals(schema.get$ref()));
            assertThat(childType3HasParentRef).isTrue();

            // Verify ChildType4 has allOf with ParentType reference
            final Schema<?> childType4Schema = generatedDescription.getComponents().getSchemas()
                    .get("org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ChildType4");
            final var childType4HasParentRef = childType4Schema.getAllOf().stream()
                    .map(s -> (Schema<?>) s)
                    .anyMatch(schema ->
                            "#/components/schemas/org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ParentType".equals(schema.get$ref()));
            assertThat(childType4HasParentRef).isTrue();

            // Verify discriminator mapping uses type aliases, not FQ class names
            final var parentTypeSchema = generatedDescription.getComponents().getSchemas()
                    .get("org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ParentType");
            final var discriminatorMapping = parentTypeSchema.getDiscriminator().getMapping();
            assertThat(discriminatorMapping)
                    .hasSize(4)
                    .containsEntry("child-type-1", "#/components/schemas/org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ChildType1")
                    .containsEntry("child-type-2", "#/components/schemas/org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ChildType2")
                    .containsEntry("child-type-3", "#/components/schemas/org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ChildType3")
                    .containsEntry("child-type-4", "#/components/schemas/org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactoryTest.ChildType4");
        } catch (AssertionError | NullPointerException e) {
            System.err.println("Test failed. Full OpenAPI description:");
            System.err.println(Json31.pretty(generatedDescription));
            throw e;
        }
    }

    @Test
    void handlesSchemaNameConflict() {
        try {
            final var schemas = generatedDescription.getComponents().getSchemas();
            assertThat(schemas.keySet())
                    .filteredOn(key -> key.contains("ConflictingResponse"))
                    .hasSize(2);
        } catch (AssertionError | NullPointerException e) {
            System.err.println("Test failed. Full OpenAPI description:");
            System.err.println(Json31.pretty(generatedDescription));
            throw e;
        }
    }

    @Test
    void usesMapSchemaForImmutableMaps() {
        try {
            // Verify ChildType1 has allOf with ParentType reference
            final Schema<?> mapSchema = generatedDescription.getPaths().get("/plugins/my.plugin.id/immutable-maps")
                    .getGet().getResponses().get("default").getContent().get("application/json").getSchema();
            assertThat(mapSchema.getTypes()).containsOnly("object");
            assertThat(mapSchema.getAdditionalProperties()).asInstanceOf(type(Schema.class))
                    .satisfies(valueSchema ->
                            assertThat(((Schema<?>) valueSchema).getTypes()).containsOnly("boolean"));
            assertThat(mapSchema.getProperties()).isNullOrEmpty();
        } catch (AssertionError | NullPointerException e) {
            System.err.println("Test failed. Full OpenAPI description:");
            System.err.println(Json31.pretty(generatedDescription));
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

    @PublicCloudAPI
    @Path("/response-schema-name-conflict")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class SchemaConflictResource implements PluginRestResource {

        @NoAuditEvent("Test")
        @GET
        @Path("pkg1")
        public org.graylog2.shared.rest.documentation.openapi.pkg1.ConflictingResponse getPkg1() {
            return new org.graylog2.shared.rest.documentation.openapi.pkg1.ConflictingResponse(true);
        }

        @NoAuditEvent("Test")
        @GET
        @Path("pkg2")
        public org.graylog2.shared.rest.documentation.openapi.pkg2.ConflictingResponse getPkg2() {
            return new org.graylog2.shared.rest.documentation.openapi.pkg2.ConflictingResponse(true);
        }
    }

    @PublicCloudAPI
    @Path("/immutable-maps")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class ImmutableMapsResource implements PluginRestResource {

        @NoAuditEvent("Test")
        @GET
        public ImmutableMap<String, Boolean> get() {
            return ImmutableMap.of("key", true);
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ChildType1.class, name = "child-type-1"),
            @JsonSubTypes.Type(value = ChildType2.class, name = "child-type-2"),
    })
    @JsonTypeName("parent-type")
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

    @JsonTypeName("child-type-3")
    public record ChildType3(double age) implements ParentType {
        @Override
        @JsonProperty("type")
        public String type() {
            return "child-type-3";
        }
    }

    @JsonTypeName("child-type-4")
    public record ChildType4(String age) implements ParentType {
        @Override
        @JsonProperty("type")
        public String type() {
            return "child-type-4";
        }
    }
}
