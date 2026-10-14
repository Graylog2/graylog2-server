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
package org.graylog2.rest.documentation.generator;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.ReadContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.PaginationParameters;
import org.graylog2.rest.resources.HelloWorldResource;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.rest.documentation.generator.Generator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratorTest {

    static ObjectMapper objectMapper;

    public record SampleEntity(@JsonProperty("type") String type) {}

    public record SampleResponse(@JsonProperty("foo") Optional<String> foo,
                                 @JsonProperty("entity") SampleEntity entity,
                                 @JsonProperty("another_entity") SampleEntity anotherEntity) {}

    @Tag(name = "Sample", description = "An example REST resource")
    @Path("/sample")
    public class SampleResource {
        @GET
        @Timed
        @Operation(summary = "A few details about the Graylog node.")
        @Produces(MediaType.APPLICATION_JSON)
        public SampleResponse sample() {
            return null;
        }
    }

    @Tag(name = "Undocumented", description = "Parameters without OpenAPI annotations")
    @Path("/undocumented")
    public class UndocumentedParametersResource {
        @PUT
        @Path("/{id}/{name}")
        @Operation(summary = "Update")
        public SampleEntity update(@PathParam("id") String id,
                                   @PathParam("name") String name,
                                   @QueryParam("variant") String variant,
                                   @Valid SampleEntity entity,
                                   @Context User user) {
            return null;
        }

        @GET
        @Operation(summary = "List")
        public List<SampleEntity> list(@Parameter(name = "pagination parameters") @BeanParam PaginationParameters paginationParameters) {
            return null;
        }
    }

    @BeforeAll
    public static void init() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Test
    public void testGenerateOverview() throws Exception {
        Generator generator = createGenerator(HelloWorldResource.class);
        Map<String, Object> result = generator.generateOverview();

        assertEquals(ServerVersion.VERSION.toString(), result.get("apiVersion"));
        assertEquals(Generator.EMULATED_SWAGGER_VERSION, result.get("swaggerVersion"));

        assertNotNull(result.get("apis"));
        assertTrue(((List) result.get("apis")).size() > 0);
    }

    @Test
    public void testGenerateForRoute() throws Exception {
        Generator generator = createGenerator(HelloWorldResource.class);
        Map<String, Object> result = generator.generateForRoute("/system", "http://localhost:12900/");
    }

    @Test
    public void testInnerClasses() throws Exception {
        final var generator = createGenerator(SampleResource.class);
        final var result = generator.generateForRoute("/sample", "http://localhost:12900/");
        assertThat(result).isNotNull();
        final var jsonResult = jsonPath(result);
        assertThat(jsonResult.read("$.apis[0].operations[0].nickname", String.class)).isEqualTo("sample");

        assertThat(jsonResult.read("$.models.GeneratorTest__SampleEntity.properties.type.type", String.class)).isEqualTo("string");
        assertThat(jsonResult.read("$.models.GeneratorTest__SampleResponse.properties.foo.type", String.class)).isEqualTo("string");
        assertThat(jsonResult.read("$.models.GeneratorTest__SampleResponse.properties.foo.required", String.class)).isEqualTo("false");
        assertThat(jsonResult.read("$.models.GeneratorTest__SampleResponse.properties.entity[\"$ref\"]", String.class)).isEqualTo("GeneratorTest__SampleEntity");
        assertThat(jsonResult.read("$.models.GeneratorTest__SampleResponse.properties.another_entity[\"$ref\"]", String.class)).isEqualTo("GeneratorTest__SampleEntity");
    }

    @Test
    public void includesParametersWithoutOpenApiAnnotations() throws Exception {
        final var result = jsonPath(createGenerator(UndocumentedParametersResource.class).generateForRoute("/undocumented", "http://localhost:12900/"));

        final List<Map<String, Object>> parameters = result.read("$.apis[?(@.path == '/undocumented/{id}/{name}')].operations[0].parameters[*]");
        assertThat(parameters).extracting(p -> p.get("name"), p -> p.get("paramType"), p -> p.get("required"))
                .containsExactly(
                        tuple("id", "path", true),
                        tuple("name", "path", true),
                        tuple("variant", "query", false),
                        tuple("JSON body", "body", true)
                );
    }

    @Test
    public void expandsBeanParametersIntoQueryParameters() throws Exception {
        final var result = jsonPath(createGenerator(UndocumentedParametersResource.class).generateForRoute("/undocumented", "http://localhost:12900/"));

        final List<Map<String, Object>> parameters = result.read("$.apis[?(@.path == '/undocumented')].operations[0].parameters[*]");
        assertThat(parameters).extracting(p -> p.get("name"), p -> p.get("paramType"), p -> p.get("defaultValue"))
                .containsExactly(
                        tuple("query", "query", null),
                        tuple("page", "query", "1"),
                        tuple("per_page", "query", "50"),
                        tuple("sort", "query", null),
                        tuple("order", "query", "asc")
                );
    }

    private Generator createGenerator(Class<?> resource) {
        return new Generator(Collections.singleton(resource), objectMapper, false, true);
    }

    private ReadContext jsonPath(Map<String, Object> result) throws JsonProcessingException {
        return com.jayway.jsonpath.JsonPath.parse(objectMapper.writeValueAsString(result));
    }
}
