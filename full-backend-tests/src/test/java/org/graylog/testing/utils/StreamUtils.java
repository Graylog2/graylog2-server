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
import io.restassured.specification.RequestSpecification;

import java.util.Collection;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public final class StreamUtils {
    private StreamUtils() {}

    public record StreamRule(@JsonProperty("type") int type,
                             @JsonProperty("value") String value,
                             @JsonProperty("field") String field,
                             @JsonProperty("inverted") boolean inverted) {}
    record CreateStreamRequest(@JsonProperty("title") String title,
                               @JsonProperty("rules") Collection<StreamRule> streamRules,
                               @JsonProperty("index_set_id") String indexSetId) {}

    public static String createStream(RequestSpecification requestSpec, String title, String indexSetId, StreamRule... streamRules) {
        final CreateStreamRequest body = new CreateStreamRequest(title, List.of(streamRules), indexSetId);
        final String streamId = given()
                .spec(requestSpec)
                .when()
                .body(body)
                .post("/streams")
                .then()
                .log().ifError()
                .statusCode(201)
                .assertThat().body("stream_id", notNullValue())
                .extract().body().jsonPath().getString("stream_id");

        given()
                .spec(requestSpec)
                .when()
                .post("/streams/" + streamId + "/resume")
                .then()
                .log().ifError()
                .statusCode(204);

        return streamId;
    }
}
