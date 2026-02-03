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
import org.apache.http.HttpStatus;

import java.util.Collection;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class Pipelines {
    private static final String URL_PREFIX = "/system/pipelines/pipeline";
    private final GraylogApis api;

    public Pipelines(GraylogApis api) {
        this.api = api;
    }

    public record Stage() {}

    private record CreatePipelineRequest(String title, String description, String source) {}

    public String create(String title, Collection<String> streamConnections) {
        return create(title, "pipeline \"" + title + "\"\nstage 0 match either\nend", streamConnections);
    }

    public String create(String title, String source, Collection<String> connectedStreams) {
        final var body = new CreatePipelineRequest(title, "", source);
        final var pipelineId = given()
                .spec(api.requestSpecification())
                .when()
                .body(body)
                .post(URL_PREFIX)
                .then()
                .log().ifError()
                .statusCode(200)
                .assertThat().body("id", notNullValue())
                .extract().body().jsonPath().getString("id");

        connect(pipelineId, connectedStreams);

        return pipelineId;
    }


    public ValidatableResponse connect(String pipelineId, Collection<String> streamIds) {
        return given()
                .spec(api.requestSpecification())
                .when()
                .body(Map.of("pipeline_id", pipelineId,
                        "stream_ids", streamIds))
                .post("/system/pipelines/connections/to_pipeline")
                .then()
                .log().ifError()
                .statusCode(200);
    }

    public ValidatableResponse delete(String id) {
        return api.delete(url(id), HttpStatus.SC_NO_CONTENT);
    }

    private String url(String suffix) {
        return URL_PREFIX + "/" + suffix;
    }
}
