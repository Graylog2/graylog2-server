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

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.IndexSetUtils;
import org.graylog.testing.utils.SearchUtils;
import org.graylog.testing.utils.StreamUtils;
import org.graylog2.plugin.streams.StreamRuleType;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.graylog.testing.completebackend.Lifecycle.VM;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = VM, searchVersions = { SearchServer.ES7, SearchServer.OS2 })
public class SuggestionResourceIT {

    static final int GELF_HTTP_PORT = 12201;

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    private String stream1Id;
    private String stream2Id;

    public SuggestionResourceIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    public void init() {
        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);
        final String defaultIndexSetId = IndexSetUtils.defaultIndexSetId(requestSpec);
        this.stream1Id = StreamUtils.createStream(requestSpec, "Stream #1", defaultIndexSetId, new StreamUtils.StreamRule(StreamRuleType.EXACT.toInteger(), "stream1", "target_stream", false));
        this.stream2Id = StreamUtils.createStream(requestSpec, "Stream #2", defaultIndexSetId, new StreamUtils.StreamRule(StreamRuleType.EXACT.toInteger(), "stream2", "target_stream", false));

        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"SuggestionResourceIT#1\", \"host\":\"example.org\", \"facility\":\"junit\", \"_target_stream\": \"stream1\"}",
                requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"SuggestionResourceIT#2\", \"host\":\"example.org\", \"facility\":\"test\", \"_target_stream\": \"stream1\"}",
                requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"SuggestionResourceIT#3\", \"host\":\"example.org\", \"facility\":\"test\", \"_target_stream\": \"stream1\"}",
                requestSpec);
        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"SuggestionResourceIT#4\", \"host\":\"foreign.org\", \"facility\":\"test\", \"_target_stream\": \"stream2\"}",
                requestSpec);
         SearchUtils.waitForMessage(requestSpec, "SuggestionResourceIT#1");
         SearchUtils.waitForMessage(requestSpec, "SuggestionResourceIT#2");
         SearchUtils.waitForMessage(requestSpec, "SuggestionResourceIT#3");
         SearchUtils.waitForMessage(requestSpec, "SuggestionResourceIT#4");
    }

    @ContainerMatrixTest
    void testMinimalRequest() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"field\":\"facility\", \"input\":\"\"}")
                .post("/search/suggest")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("suggestions.value[0]", equalTo("test"));
        validatableResponse.assertThat().body("suggestions.occurrence[0]", greaterThanOrEqualTo(3));
    }

    @ContainerMatrixTest
    void testSuggestionsAreLimitedToStream() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body(Map.of(
                        "field", "source",
                        "input", "",
                        "streams", Set.of(stream1Id)
                ))
                .post("/search/suggest")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("suggestions.value[0]", equalTo("example.org"));
        validatableResponse.assertThat().body("suggestions.occurrence[0]", equalTo(3));

        final ValidatableResponse validatableResponse2 = given()
                .spec(requestSpec)
                .when()
                .body(Map.of(
                        "field", "source",
                        "input", "",
                        "streams", Set.of(stream2Id)
                ))
                .post("/search/suggest")
                .then()
                .statusCode(200);
        validatableResponse2.assertThat().body("suggestions.value[0]", equalTo("foreign.org"));
        validatableResponse2.assertThat().body("suggestions.occurrence[0]", equalTo(1));
    }

    @ContainerMatrixTest
    void testInvalidField() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"field\":\"message\", \"input\":\"foo\"}")
                .post("/search/suggest")
                .then()
                .statusCode(200);
        // error types and messages are different for each ES version, so let's just check that there is an error in the response
        validatableResponse.assertThat().body("error", notNullValue());
    }

    @ContainerMatrixTest
    void testSizeOtherDocsCount() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"field\":\"facility\", \"input\":\"\", \"size\":1}")
                .post("/search/suggest")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("suggestions.value[0]", equalTo("test"));
        validatableResponse.assertThat().body("suggestions.occurrence[0]", greaterThanOrEqualTo(2));
        validatableResponse.assertThat().body("sum_other_docs_count", greaterThanOrEqualTo(1));
    }

    @ContainerMatrixTest
    void testTypoCorrection() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("{\"field\":\"facility\", \"input\":\"tets\"}")
                .post("/search/suggest")
                .then()
                .statusCode(200);
        validatableResponse.assertThat().body("suggestions.value[0]", equalTo("test"));
        validatableResponse.assertThat().body("suggestions.occurrence[0]", greaterThanOrEqualTo(1));
    }

}
