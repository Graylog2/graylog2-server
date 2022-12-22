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
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.completebackend.apis.inputs.GelfInputApi;

import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.not;

public class GraylogApis {
    private final RequestSpecification requestSpecification;
    private final GraylogBackend backend;
    private final Users users;
    private final Streams streams;
    private final Sharing sharing;
    private final GelfInputApi gelf;
    private final Search search;
    private final Indices indices;
    private final FieldTypes fieldTypes;

    public GraylogApis(RequestSpecification requestSpecification, GraylogBackend backend) {
        this.requestSpecification = requestSpecification;
        this.backend = backend;
        this.users = new Users(this.requestSpecification);
        this.streams = new Streams(this.requestSpecification);
        this.sharing = new Sharing(this.requestSpecification);
        this.gelf = new GelfInputApi(this.requestSpecification, backend);
        this.search = new Search(this.requestSpecification);
        this.indices = new Indices(this.requestSpecification);
        this.fieldTypes = new FieldTypes(this.requestSpecification);
    }

    public RequestSpecification requestSpecification() {
        return requestSpecification;
    }

    public GraylogBackend backend() {
        return backend;
    }

    public Users users() {
        return users;
    }

    public Streams streams() {
        return streams;
    }

    public Sharing sharing() {
        return sharing;
    }

    public GelfInputApi gelf() {
        return gelf;
    }

    public Search search() {
        return this.search;
    }

    public Indices indices() {
        return indices;
    }

    public FieldTypes fieldTypes() {
        return this.fieldTypes;
    }

    public ValidatableResponse postWithResource(final String url, final String resource, final int expectedResult) {
        return given()
                .spec(requestSpecification)
                .auth().basic("admin", "admin")
                .when()
                .body(getClass().getClassLoader().getResourceAsStream(resource))
                .post(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    public ValidatableResponse post(final String url, final String body, final int expectedResult) {
        var response =  given()
                .spec(requestSpecification)
                .auth().basic("admin", "admin")
                .when();

        if(body != null) {
            response = response.body(body);
        }

        return response
                .post(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    public ValidatableResponse get(final String url, final String username, final String password, final int expectedResult) {
        return given()
                .spec(requestSpecification)
                .auth().basic(username, password)
                .when()
                .get(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    public ValidatableResponse get(final String url, final int expectedResult) {
        return get(url, "admin", "admin", expectedResult);
    }
}
