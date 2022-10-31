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
import com.google.auto.value.AutoValue;
import io.restassured.specification.RequestSpecification;

import java.util.Arrays;
import java.util.Collection;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public final class StreamUtils {
    private StreamUtils() {}

    @AutoValue
    public abstract static class StreamRule {
        @JsonProperty("type")
        public abstract int type();
        @JsonProperty("value")
        public abstract String value();
        @JsonProperty("field")
        public abstract String field();
        @JsonProperty("inverted")
        public abstract boolean inverted();
        public static StreamRule create(@JsonProperty("type") int type,
                                        @JsonProperty("value") String value,
                                        @JsonProperty("field") String field,
                                        @JsonProperty("inverted") boolean inverted) {
            return new AutoValue_StreamUtils.StreamRule(type, value, field, inverted);
        }
    }

    public abstract static class CreateStreamRequest {
        @JsonProperty("title")
        public abstract String title();
        @JsonProperty("rules")
        public abstract Collection<StreamRule> streamRules();
        @JsonProperty("index_set_id")
        public abstract String indexSetId();
        public static CreateStreamRequest create(@JsonProperty("title") String title,
                                                 @JsonProperty("rules") Collection<StreamRule> streamRules,
                                                 @JsonProperty("index_set_id") String indexSetId) {
            return new AutoValue_StreamUtils.CreateStreamRequest(title, streamRules, indexSetId);
        }
    }

    public static String createStream(RequestSpecification requestSpec, String title, String indexSetId, StreamRule... streamRules) {
        final CreateStreamRequest body = CreateStreamRequest.create(title, Arrays.asList(streamRules), indexSetId);
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
