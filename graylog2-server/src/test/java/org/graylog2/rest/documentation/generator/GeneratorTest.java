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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog2.rest.resources.HelloWorldResource;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.rest.documentation.generator.Generator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GeneratorTest {

    static ObjectMapper objectMapper;

    public record SampleEntity(@JsonProperty("type") String type) {}

    public record SampleResponse(@JsonProperty("foo") Optional<String> foo,
                                 @JsonProperty("entity") SampleEntity entity,
                                 @JsonProperty("another_entity") SampleEntity anotherEntity) {}

    @Api(value = "Sample", description = "An example REST resource")
    @Path("/sample")
    public class SampleResource {
        @GET
        @Timed
        @ApiOperation(value = "A few details about the Graylog node.")
        @Produces(MediaType.APPLICATION_JSON)
        public SampleResponse sample() {
            return null;
        }
    }

    @BeforeClass
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
        assertThat(jsonResult.read("$.models.GeneratorTest__SampleResponse.properties.entity[\"$ref\"]", String.class)).isEqualTo("GeneratorTest__SampleEntity");
        assertThat(jsonResult.read("$.models.GeneratorTest__SampleResponse.properties.another_entity[\"$ref\"]", String.class)).isEqualTo("GeneratorTest__SampleEntity");
    }

    private Generator createGenerator(Class<?> resource) {
        return new Generator(Collections.singleton(resource), objectMapper, false, true);
    }

    private ReadContext jsonPath(Map<String, Object> result) throws JsonProcessingException {
        return com.jayway.jsonpath.JsonPath.parse(objectMapper.writeValueAsString(result));
    }
}
