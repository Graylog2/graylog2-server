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
package org.graylog.plugins.views;

import com.google.common.collect.ImmutableMap;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import java.util.Collections;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

@ContainerMatrixTestsConfiguration(searchVersions = {SearchServer.OS1}, mongoVersions = {MongodbServer.MONGO5})
public class SessionsResourceIT {
    private static final String SESSIONS_RESOURCE = "/system/sessions";
    private static final String AUTHENTICATION_COOKIE = "authentication";
    private static final Map<String, String> VALID_CREDENTIALS = ImmutableMap.of(
            "username", "admin",
            "password", "admin"
    );
    private static final Map<String, String> INVALID_CREDENTIALS = ImmutableMap.of(
            "username", "admin",
            "password", "wrongpassword"
    );

    private final RequestSpecification requestSpec;
    private final GraylogBackend backend;

    private static RequestSpecification makeRequestSpec(GraylogBackend backend) {
        return new RequestSpecBuilder().build()
                .baseUri(backend.uri())
                .port(backend.apiPort())
                .basePath("/api")
                .accept(JSON)
                .contentType(JSON)
                .header("X-Requested-By", "peterchen");
    }

    public SessionsResourceIT(GraylogBackend backend) {
        this.requestSpec = makeRequestSpec(backend);
        this.backend = backend;
    }

    @ContainerMatrixTest
    void failingLoginShouldNotReturnCookieOrToken() {
        given()
                .spec(requestSpec)
                .post(SESSIONS_RESOURCE)
                .then()
                .assertThat()
                .statusCode(400)
                .cookies(Collections.emptyMap());

        given()
                .spec(requestSpec)
                .body(INVALID_CREDENTIALS)
                .post(SESSIONS_RESOURCE)
                .then()
                .assertThat()
                .statusCode(401)
                .cookies(Collections.emptyMap());
    }

    @ContainerMatrixTest
    void successfulLoginShouldReturnCookieAndToken() {
        final Response response = given()
                .spec(requestSpec)
                .body(VALID_CREDENTIALS)
                .post(SESSIONS_RESOURCE);

        response.then()
                .assertThat()
                .statusCode(200)
                .body("session_id", not(emptyOrNullString()))
                .cookie(AUTHENTICATION_COOKIE, not(emptyOrNullString()));

        assertThat(response.jsonPath().getString("session_id"))
                .isEqualTo(response.cookie(AUTHENTICATION_COOKIE));

        final Cookie authenticationCookie = response.getDetailedCookie(AUTHENTICATION_COOKIE);
        final RequestSpecification authenticatedRequest = makeRequestSpec(backend).cookie(authenticationCookie);

        given()
                .spec(authenticatedRequest)
                .get("/system")
                .then()
                .assertThat()
                .statusCode(200);
    }
}
