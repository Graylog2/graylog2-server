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
package org.graylog.testing.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;

import java.util.List;

import static io.restassured.RestAssured.given;

public class UserUtils {

    public static record User(@JsonProperty("username") String username,
                              @JsonProperty("password") String password,
                              @JsonProperty("first_name") String firstName,
                              @JsonProperty("last_name") String lastName,
                              @JsonProperty("email") String email,
                              @JsonProperty("service_account") boolean serviceAccount,
                              @JsonProperty("session_timeout_ms") long sessionTimeoutMs,
                              @JsonProperty("timezone") String timezone,
                              @JsonProperty("roles") List<String> roles,
                              @JsonProperty("permissions") List<String> permissions
                              ) {
    }

    public static JsonPath createUser(RequestSpecification requestSpec, User user) {
        given()
                .spec(requestSpec)
                .when()
                .body(user)
                .post("/users")
                .then()
                .log().ifError()
                .statusCode(201);

        return getUserInfo(requestSpec, user.username);
    }

    public static JsonPath getUserInfo(RequestSpecification requestSpec, String username) {
        return given()
                .spec(requestSpec)
                .when()
                .get("/users/" + username)
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().jsonPath();
    }
}
