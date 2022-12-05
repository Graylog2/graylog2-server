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
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.IndexSetUtils;
import org.graylog.testing.utils.StreamUtils;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ContainerMatrixTestsConfiguration(mongoVersions = MongodbServer.MONGO5, serverLifecycle = Lifecycle.CLASS)
public class StreamsIT {
    private static final String STREAMS_RESOURCE = "/streams";

    private final RequestSpecification requestSpec;

    public StreamsIT(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    void beforeAll() {
        final String defaultIndexSetId = IndexSetUtils.defaultIndexSetId(requestSpec);
        final String newIndexSetId = IndexSetUtils.createIndexSet(requestSpec, "Test Indices", "Some test indices", "streamstest");
        final String newIndexSetId2 = IndexSetUtils.createIndexSet(requestSpec, "More Test Indices", "Some more test indices", "moretest");
        StreamUtils.createStream(requestSpec, "New Stream", newIndexSetId);
        StreamUtils.createStream(requestSpec, "New Stream 2", defaultIndexSetId);
        StreamUtils.createStream(requestSpec, "New Stream 3", newIndexSetId2);
    }

    @ContainerMatrixTest
    void sortByIndexSetTitle() {
        paginatedByFieldWithOrder("title", "asc")
                .assertThat()
                .body("streams*.title", equalTo(List.of("New Stream", "New Stream 2", "New Stream 3")));
        paginatedByFieldWithOrder("title", "desc")
                .assertThat()
                .body("streams*.title", equalTo(List.of("New Stream 3", "New Stream 2", "New Stream")));
        paginatedByFieldWithOrder("index_set_title", "asc")
                .assertThat()
                .body("streams*.title", equalTo(List.of("New Stream 2", "New Stream 3", "New Stream")));
        paginatedByFieldWithOrder("index_set_title", "desc")
                .assertThat()
                .body("streams*.title", equalTo(List.of("New Stream", "New Stream 3", "New Stream 2")));
    }

    private ValidatableResponse paginatedByFieldWithOrder(String field, String order) {
        return given()
                .spec(requestSpec)
                .log().ifValidationFails()
                .when()
                .queryParam("query", "New")
                .queryParam("sort", field)
                .queryParam("order", order)
                .get(STREAMS_RESOURCE + "/paginated")
                .then()
                .log().ifValidationFails();
    }
}
