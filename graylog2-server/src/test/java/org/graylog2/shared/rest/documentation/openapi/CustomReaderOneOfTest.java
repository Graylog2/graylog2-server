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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomReaderOneOfTest {

    @Test
    void subtypesOfOneOfParentInlineParentInsteadOfReferencingIt() {
        final var reader = new CustomReader(Map.of(), new SwaggerConfiguration().openAPI31(true));
        final var schemas = reader.read(Set.of(ShapeResource.class), Map.of()).getComponents().getSchemas();

        final var parent = schemas.entrySet().stream()
                .filter(e -> e.getKey().endsWith("Shape"))
                .findFirst().orElseThrow().getValue();
        assertThat(parent.getOneOf()).hasSize(2);

        final List<io.swagger.v3.oas.models.media.Schema<?>> circleAllOf = schemas.entrySet().stream()
                .filter(e -> e.getKey().endsWith("Circle"))
                .findFirst().orElseThrow().getValue().getAllOf();
        assertThat(circleAllOf).noneMatch(s -> s.get$ref() != null && s.get$ref().endsWith("Shape"));
        assertThat(circleAllOf).anySatisfy(s -> assertThat(s.getProperties()).containsKey("type"));
        assertThat(circleAllOf).anySatisfy(s -> assertThat(s.getProperties()).containsKey("radius"));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Circle.class, name = "circle"),
            @JsonSubTypes.Type(value = Square.class, name = "square"),
    })
    @Schema(discriminatorProperty = "type", oneOf = {Circle.class, Square.class})
    public interface Shape {
        @JsonProperty("type")
        String type();
    }

    public record Circle(@JsonProperty("type") String type, @JsonProperty("radius") double radius) implements Shape {}

    public record Square(@JsonProperty("type") String type, @JsonProperty("side") double side) implements Shape {}

    @Path("/shapes")
    public static class ShapeResource {
        @GET
        public Shape get() {
            return null;
        }
    }
}
