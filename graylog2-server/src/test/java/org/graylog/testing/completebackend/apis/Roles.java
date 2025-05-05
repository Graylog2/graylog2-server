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
package org.graylog.testing.completebackend.apis;

import io.restassured.response.ValidatableResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;
import org.graylog2.rest.models.roles.responses.RoleResponse;

import java.util.Optional;
import java.util.Set;

import static io.restassured.RestAssured.given;

public class Roles implements GraylogRestApi {

    private final GraylogApis api;

    public Roles(GraylogApis api) {
        this.api = api;
    }

    public GraylogApiResponse createRole(@NotBlank String name, String description, @NotNull Set<String> permissions, boolean readOnly) {
        final ValidatableResponse result = given()
                .spec(api.requestSpecification())
                .when()
                .body(RoleResponse.create(name, Optional.ofNullable(description), permissions, readOnly))
                .post("/roles")
                .then()
                .log().ifError()
                .statusCode(Response.Status.CREATED.getStatusCode());
        return new GraylogApiResponse(result);
    }
}
