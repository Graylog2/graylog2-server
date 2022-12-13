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

import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;

public class Sharing implements GraylogRestApi {

    public static final String ENTITY_STREAM = "stream";
    public static final String ENTITY_USER = "user";

    public static final String PERMISSION_OWN = "own";
    public static final String PERMISSION_VIEW = "view";
    private final RequestSpecification requestSpecification;

    public Sharing(RequestSpecification requestSpecification) {
        this.requestSpecification = requestSpecification;
    }


    public JsonPath setSharing(SharingRequest req) {
        return given()
                .spec(this.requestSpecification)
                .when()
                .body(serialize(req))
                .post("/authz/shares/entities/" + req.entity().serialize())
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().jsonPath();
    }

    private static Object serialize(SharingRequest req) {
        return ImmutableMap.of("selected_grantee_capabilities", req.permissions().entrySet().stream().collect(Collectors.toMap(
                it -> it.getKey().serialize(),
                Map.Entry::getValue
        )));
    }
}
