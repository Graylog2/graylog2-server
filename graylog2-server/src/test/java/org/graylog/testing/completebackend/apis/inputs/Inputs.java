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
package org.graylog.testing.completebackend.apis.inputs;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.restassured.response.ValidatableResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.GraylogRestApi;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class Inputs implements GraylogRestApi {

    private final GraylogApis api;

    public Inputs(GraylogApis api) {
        this.api = api;
    }

    record CreateInputRequest(@JsonProperty("title") String title,
                              @JsonProperty("type") String type,
                              @JsonProperty("global") boolean global,
                              @JsonProperty("configuration") Map<String, Object> configuration) {}

    public String createGlobalInput(String title, String type, Map<String, Object> configuration) {
        final CreateInputRequest body = new CreateInputRequest(title, type, true, configuration);
        return given().
                spec(api.requestSpecification())
                .when()
                .body(body)
                .post("/system/inputs")
                .then()
                .log().ifError()
                .statusCode(201)
                .assertThat().body("id", notNullValue())
                .extract().body().jsonPath().getString("id");
    }

    public ValidatableResponse getInput(String inputId) {
        return given()
                .spec(api.requestSpecification())
                .when()
                .get("/system/inputs/" + inputId)
                .then()
                .log().ifError()
                .statusCode(200);
    }

    public ValidatableResponse getInputState(String inputId) {
        return given()
                .spec(api.requestSpecification())
                .when()
                .get("/system/inputstates/" + inputId)
                .then()
                .log().ifError()
                .statusCode(200);
    }

    public ValidatableResponse deleteInput(String inputId) {
        return given()
                .spec(api.requestSpecification())
                .when()
                .delete("/system/inputs/" + inputId)
                .then()
                .log().ifError()
                .statusCode(204);
    }
}
