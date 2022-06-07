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

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.SearchUtils;
import org.junit.jupiter.api.BeforeAll;

import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration
public class SearchSyncIT {
    static final int GELF_HTTP_PORT = 12201;

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public SearchSyncIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    public void importMongoFixtures() {
        this.sut.importMongoDBFixture("mongodb-stored-searches-for-execution-endpoint.json", SearchSyncIT.class);

        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);
        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"search-sync-test\", \"host\":\"example.org\", \"facility\":\"test\"}",
                requestSpec);

        // mainly because of the waiting logic
        final boolean isMessagePresent = SearchUtils.waitForMessage(requestSpec, "search-sync-test");
        assertThat(isMessagePresent).isTrue();
    }

    @ContainerMatrixTest
    void testEmptyBody() {
        given()
                .spec(requestSpec)
                .when()
                .post("/views/search/sync")
                .then()
                .statusCode(400)
                .assertThat().body("message[0]", equalTo("Search body is mandatory"));
    }

    @ContainerMatrixTest
    void testMinimalisticRequest() {
        given()
                .spec(requestSpec)
                .when()
                .body(fixture("org/graylog/plugins/views/minimalistic-request.json"))
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .assertThat()
                .body("execution.completed_exceptionally", equalTo(false))
                .body("results*.value.search_types[0]*.value.messages.message.message[0]", hasItem("search-sync-test"));
    }

    @ContainerMatrixTest
    void testMinimalisticRequestv2() {
        given()
                .spec(requestSpec)
                .accept("application/vnd.graylog.search.v2+json")
                .contentType("application/vnd.graylog.search.v2+json")
                .when()
                .body(fixture("org/graylog/plugins/views/minimalistic-request.json"))
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .assertThat()
                .body("execution.completed_exceptionally", equalTo(false))
                .body("results*.value.search_types[0]*.value.messages.message.message[0]", hasItem("search-sync-test"));
    }

    @ContainerMatrixTest
    void testRequestWithStreamsv2() {
        given()
                .spec(requestSpec)
                .accept("application/vnd.graylog.search.v2+json")
                .contentType("application/vnd.graylog.search.v2+json")
                .when()
                .body(fixture("org/graylog/plugins/views/minimalistic-request-with-streams.json"))
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .assertThat()
                .body("execution.completed_exceptionally", equalTo(false))
                .body("results*.value.search_types[0]*.value.messages.message.message[0]", hasItem("search-sync-test"));
    }

    @ContainerMatrixTest
    void testRequestStoredSearch() throws ExecutionException, RetryException {
        final String jobId = executeStoredSearch("61977043c1f17d26b45c8a0b");

        retrieveSearchResults(jobId)
                .body("execution.completed_exceptionally", equalTo(false))
                .body("results.f1446410-a082-4871-b3bf-d69aa42d0c96.search_types.8306779b-933f-473f-837d-b7a7d83a9a40.name", equalTo("chart"));
    }

    @ContainerMatrixTest
    void testRequestStoredSearchWithGlobalOverrideKeepingOnlySingleSearchType() throws ExecutionException, RetryException {
        final String jobId = executeStoredSearch("61977043c1f17d26b45c8a0b", Collections.singletonMap(
                "global_override", Collections.singletonMap(
                        "keep_search_types", Collections.singleton("01c76680-377b-4930-86e2-a55fdb867b58")
                )
        ));

        retrieveSearchResults(jobId)
                .body("execution.completed_exceptionally", equalTo(false))
                .body("results.f1446410-a082-4871-b3bf-d69aa42d0c96.search_types", not(hasKey("f1446410-a082-4871-b3bf-d69aa42d0c97")))
                .body("results.f1446410-a082-4871-b3bf-d69aa42d0c97.search_types", hasKey("01c76680-377b-4930-86e2-a55fdb867b58"));
    }

    @ContainerMatrixTest
    void testThatQueryOrderStaysConsistentInV1() {
        given()
                .spec(requestSpec)
                .accept("application/json")
                .contentType("application/json")
                .when()
                .body(fixture("org/graylog/plugins/views/search-with-three-empty-queries.json"))
                .post("/views/search")
                .then()
                .statusCode(201)
                .assertThat()
                .body("queries*.id", contains("4966dd79-2c7d-4ba9-8f90-c84aea7b5c49",
                        "0d5b45b8-1f55-4b60-ad34-d086ddd5d8fa",
                        "3eec6f5c-0f1b-41dc-bb95-3ebc6bb905f3"));
    }

    @ContainerMatrixTest
    void testThatQueryOrderStaysConsistentInV2() {
        given()
                .spec(requestSpec)
                .accept("application/vnd.graylog.search.v2+json")
                .contentType("application/vnd.graylog.search.v2+json")
                .when()
                .body(fixture("org/graylog/plugins/views/search-with-three-empty-queries-v2.json"))
                .post("/views/search")
                .then()
                .statusCode(201)
                .assertThat()
                .body("queries*.id", contains("4966dd79-2c7d-4ba9-8f90-c84aea7b5c49",
                        "0d5b45b8-1f55-4b60-ad34-d086ddd5d8fa",
                        "3eec6f5c-0f1b-41dc-bb95-3ebc6bb905f3"));
    }

    private String executeStoredSearch(String searchId) {
        return executeStoredSearch(searchId, Collections.emptyMap());
    }

    private String executeStoredSearch(String searchId, Object body) {
        final ValidatableResponse result = given()
                .spec(requestSpec)
                .when()
                .body(body)
                .post("/views/search/{searchId}/execute", searchId)
                .then()
                .statusCode(201);

        final String jobId = result.extract().path("id");

        assertThat(jobId).isNotBlank();

        return jobId;
    }

    private ValidatableResponse retrieveSearchResults(String jobId) throws ExecutionException, RetryException {
        final Retryer<ValidatableResponse> retryer = RetryerBuilder.<ValidatableResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(5))
                .build();

        return retryer.call(() -> given()
                .spec(requestSpec)
                .when()
                .get("/views/search/status/{jobId}", jobId)
                .then()
                .statusCode(200)
                .body("execution.done", equalTo(true)));
    }

    private InputStream fixture(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }
}
