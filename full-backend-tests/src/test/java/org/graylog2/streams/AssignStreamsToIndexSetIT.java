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
package org.graylog2.streams;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.IndexSetUtils;
import org.graylog.testing.utils.StreamUtils;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collection;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ContainerMatrixTestsConfiguration(mongoVersions = MongodbServer.MONGO5)
public class AssignStreamsToIndexSetIT {
    private static final String STREAMS_RESOURCE = "/streams";

    private final RequestSpecification requestSpec;

    public AssignStreamsToIndexSetIT(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    private String defaultIndexSetId;
    private String newIndexSetId;
    private String stream1Id;
    private String stream2Id;
    private String stream3Id;

    @BeforeAll
    void beforeAll() {
        this.defaultIndexSetId = IndexSetUtils.defaultIndexSetId(requestSpec);
        this.stream1Id = StreamUtils.createStream(requestSpec, "New Stream", defaultIndexSetId);
        this.stream2Id = StreamUtils.createStream(requestSpec, "New Stream 2", defaultIndexSetId);
        this.stream3Id = StreamUtils.createStream(requestSpec, "New Stream 3", defaultIndexSetId);
        this.newIndexSetId = IndexSetUtils.createIndexSet(requestSpec, "Test Indices", "Some test indices", "test");
    }

    @ContainerMatrixTest
    void assignStreamsToIndexSet() {
        assignToIndexSet(List.of(stream1Id, stream2Id, stream3Id), newIndexSetId)
                .statusCode(200);

        StreamUtils.getStream(requestSpec, stream1Id)
                .assertThat().body("index_set_id", equalTo(newIndexSetId));
        StreamUtils.getStream(requestSpec, stream2Id)
                .assertThat().body("index_set_id", equalTo(newIndexSetId));
        StreamUtils.getStream(requestSpec, stream3Id)
                .assertThat().body("index_set_id", equalTo(newIndexSetId));

        assignToIndexSet(List.of(stream1Id, stream2Id, stream3Id), defaultIndexSetId)
                .statusCode(200);

        StreamUtils.getStream(requestSpec, stream1Id)
                .assertThat().body("index_set_id", equalTo(defaultIndexSetId));
        StreamUtils.getStream(requestSpec, stream2Id)
                .assertThat().body("index_set_id", equalTo(defaultIndexSetId));
        StreamUtils.getStream(requestSpec, stream3Id)
                .assertThat().body("index_set_id", equalTo(defaultIndexSetId));
    }

    @ContainerMatrixTest
    void assignStreamsToMissingIndexSet() {
        assignToIndexSet(List.of(stream1Id, stream2Id, stream3Id), "doesnotexist")
                .statusCode(404);

        StreamUtils.getStream(requestSpec, stream1Id)
                .assertThat().body("index_set_id", equalTo(defaultIndexSetId));
        StreamUtils.getStream(requestSpec, stream2Id)
                .assertThat().body("index_set_id", equalTo(defaultIndexSetId));
        StreamUtils.getStream(requestSpec, stream3Id)
                .assertThat().body("index_set_id", equalTo(defaultIndexSetId));
    }

    @ContainerMatrixTest
    void assignMissingStreamToIndexSet() {
        assignToIndexSet(List.of(stream1Id, stream2Id, stream3Id, "6389c6a9205a90634f992bce"), newIndexSetId)
                .statusCode(404);

        StreamUtils.getStream(requestSpec, stream1Id)
                .assertThat().body("index_set_id", equalTo(defaultIndexSetId));
        StreamUtils.getStream(requestSpec, stream2Id)
                .assertThat().body("index_set_id", equalTo(defaultIndexSetId));
        StreamUtils.getStream(requestSpec, stream3Id)
                .assertThat().body("index_set_id", equalTo(defaultIndexSetId));
    }

    private ValidatableResponse assignToIndexSet(Collection<String> streamIds, String indexSetId) {
        return given()
                .spec(requestSpec)
                .log().ifValidationFails()
                .when()
                .body(streamIds)
                .put(STREAMS_RESOURCE + "/indexSet/" + indexSetId)
                .then()
                .log().ifValidationFails();
    }
}
