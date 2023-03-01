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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public final class Streams implements GraylogRestApi {

    private final RequestSpecification requestSpecification;

    public Streams(RequestSpecification requestSpecification) {
        this.requestSpecification = requestSpecification;
    }

    public record StreamRule(@JsonProperty("type") int type,
                             @JsonProperty("value") String value,
                             @JsonProperty("field") String field,
                             @JsonProperty("inverted") boolean inverted) {}
    record CreateStreamRequest(@JsonProperty("title") String title,
                               @JsonProperty("rules") Collection<StreamRule> streamRules,
                               @JsonProperty("index_set_id") String indexSetId) {}

    public String createStream(String title, String indexSetId, StreamRule... streamRules) {
        return waitForStreamRouterRefresh(() -> createStream(title, indexSetId, true, streamRules));
    }

    public String createStream(String title, String indexSetId, boolean started, StreamRule... streamRules) {
        final CreateStreamRequest body = new CreateStreamRequest(title, List.of(streamRules), indexSetId);
        final String streamId = given()
                .spec(this.requestSpecification)
                .when()
                .body(body)
                .post("/streams")
                .then()
                .log().ifError()
                .statusCode(201)
                .assertThat().body("stream_id", notNullValue())
                .extract().body().jsonPath().getString("stream_id");

        if (started) {
            given()
                    .spec(this.requestSpecification)
                    .when()
                    .post("/streams/" + streamId + "/resume")
                    .then()
                    .log().ifError()
                    .statusCode(204);
        }

        return streamId;
    }

    public ValidatableResponse getStream(String streamId) {
        return given()
                .spec(this.requestSpecification)
                .when()
                .get("/streams/" + streamId)
                .then()
                .log().ifError()
                .statusCode(200)
                .assertThat().body("id", equalTo(streamId));
    }

    private String getStreamRouterEngineFingerprint() {
        return given()
                .spec(this.requestSpecification)
                .when()
                .get("/system/debug/streams/router_engine_info")
                .then()
                .extract().body().path("fingerprint");

    }


    /**
     * Stream creation is an async operation. The stream router engine has to be refreshed in a separated thread.
     * This may lead to unpredictable results, when messages are persisted immediately after stream creation.
     * This wait wrapper will check the stream router engine fingerprint before the operation and delays
     * returning result of the operation till the fingerprint is actually changed and new rules applied.
     */
    private <T> T waitForStreamRouterRefresh(Supplier<T> operation) {
        final String fingerprintBeforeOp = getStreamRouterEngineFingerprint();
        final T result = operation.get();
        waitForStreamRouterEngineRefresh(fingerprintBeforeOp);
        return result;
    }

    private void waitForStreamRouterEngineRefresh(String existingEngineFingerprint) {
        try {
            RetryerBuilder.<String>newBuilder()
                    .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS))
                    .withStopStrategy(StopStrategies.stopAfterDelay(10, TimeUnit.SECONDS))
                    .retryIfResult(r -> r.equals(existingEngineFingerprint))
                    .build()
                    .call(this::getStreamRouterEngineFingerprint);
        } catch (ExecutionException | RetryException e) {
            throw new RuntimeException("Failed to wait for stream router engine refresh, fingerprint hasn't changed", e);
        }
    }
}
