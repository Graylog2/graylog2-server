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

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.response.ValidatableResponse;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetCreationRequest;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class Indices implements GraylogRestApi {
    private final GraylogApis api;

    public Indices(GraylogApis api) {
        this.api = api;
    }

    public String defaultIndexSetId() {
        return given()
                .spec(api.requestSpecification())
                .when()
                .get("/system/indices/index_sets")
                .then()
                .statusCode(200)
                .assertThat()
                .extract().body().jsonPath().getString("index_sets.find { it.default == true }.id");
    }

    public String createIndexSet(IndexSetCreationRequest indexSetCreationRequest) throws ExecutionException, RetryException {
        final var id = given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .body(indexSetCreationRequest)
                .post("/system/indices/index_sets")
                .then()
                .log().ifError()
                .log().ifValidationFails()
                .statusCode(200)
                .assertThat().body("id", notNullValue())
                .extract().body().jsonPath().getString("id");
        waitForIndexNames(id);
        waitForDeflector(id);
        return id;
    }

    public String createIndexSet(String title, String description, String prefix) throws ExecutionException, RetryException {
        var indexSetSummary = IndexSetCreationRequest.builder()
                .title(title)
                .description(description)
                .isWritable(true)
                .indexPrefix(prefix)
                .shards(4)
                .replicas(0)
                .rotationStrategyClass("org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy")
                .rotationStrategyConfig(TimeBasedRotationStrategyConfig.builder()
                        .rotationPeriod(Period.days(1))
                        .rotateEmptyIndexSet(false)
                        .build())
                .retentionStrategyClass("org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy")
                .retentionStrategyConfig(DeletionRetentionStrategyConfig.create(20))
                .creationDate(ZonedDateTime.now(ZoneId.of("UTC")))
                .indexAnalyzer("standard")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .fieldTypeRefreshInterval(Duration.standardSeconds(5L))
                .indexTemplateType(null)
                .fieldTypeProfile(null)
                .dataTieringConfig(null)
                .useLegacyRotation(true)
                .build();

        return createIndexSet(indexSetSummary);
    }

    // fails with a 404 if the index set does not exist
    public GraylogApiResponse listOpenIndices(String indexSetId) {
        final ValidatableResponse response = given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .get("/system/indexer/indices/" + indexSetId + "/open")
                .then()
                .log().ifError()
                .log()
                .ifValidationFails()
                .statusCode(200);
        return new GraylogApiResponse(response);
    }

    // can be used as in "waitForIndexNames", does not fail if the index set does not exist/is not yet available
    public List<String> listOpenIndicesWithEmptyResultOnError(String indexSetId) {
        final var response = given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .get("/system/indexer/indices/" + indexSetId + "/open");

        if(response.statusCode() == 200) {
            return new GraylogApiResponse(response.then()).properJSONPath().read("indices.*.index_name");
        } else {
            return List.of();
        }
    }

    public List<String> waitForIndexNames(String indexSetId) throws ExecutionException, RetryException {
        return RetryerBuilder.<List<String>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(30))
                .retryIfResult(List::isEmpty)
                .build()
                .call(() -> listOpenIndicesWithEmptyResultOnError(indexSetId));
    }

    private boolean isDeflectorUp(String indexSetId) {
        final var response = given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .get("/system/indexer/overview/" + indexSetId);
        if (response.statusCode() == 200) {
            return new GraylogApiResponse(response.then()).properJSONPath().read("deflector.is_up", Boolean.class);
        } else {
            return false;
        }
    }

    private void waitForDeflector(String indexSetId) throws ExecutionException, RetryException {
        RetryerBuilder.<Boolean>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(30))
                .retryIfResult(result -> result == null || result.equals(false))
                .build()
                .call(() -> isDeflectorUp(indexSetId));
    }

    public void rotateIndexSet(String indexSetId) {
        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .post("/system/deflector/" + indexSetId + "/cycle")
                .then()
                .log().ifError()
                .log()
                .ifValidationFails()
                .statusCode(204);
    }

    public void deleteIndexSet(String indexSetId, boolean deleteIndices) {
        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .param("delete_indices", deleteIndices)
                .delete("/system/indices/index_sets/" + indexSetId)
                .then()
                .log().ifError()
                .log().ifValidationFails()
                .statusCode(204);
    }

    public void deleteIndex(String index) {
        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .delete("/system/indexer/indices/" + index)
                .then()
                .log().ifError()
                .log().ifValidationFails()
                .statusCode(204);
    }

    public GraylogApiResponse listIndexRanges() {
        final ValidatableResponse response = given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .get("/system/indices/ranges")
                .then()
                .log().ifError()
                .log()
                .ifValidationFails()
                .statusCode(200);
        return new GraylogApiResponse(response);
    }

    public void rebuildIndexRanges() {
        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .post("/system/indices/ranges/rebuild")
                .then()
                .log().ifError()
                .log()
                .ifValidationFails()
                .statusCode(202);
    }
}
