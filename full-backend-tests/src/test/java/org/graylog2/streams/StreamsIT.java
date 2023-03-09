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
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.graylog2.rest.models.tools.responses.PageListResponse.ELEMENTS_FIELD_NAME;
import static org.hamcrest.Matchers.equalTo;

@ContainerMatrixTestsConfiguration(mongoVersions = MongodbServer.MONGO5, serverLifecycle = Lifecycle.CLASS)
public class StreamsIT {
    private static final String STREAMS_RESOURCE = "/streams";

    private final GraylogApis api;

    public StreamsIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeAll
    void beforeAll() {
        final String defaultIndexSetId = api.indices().defaultIndexSetId();
        final String newIndexSetId = api.indices().createIndexSet("Test Indices", "Some test indices", "streamstest");
        final String newIndexSetId2 = api.indices().createIndexSet("More Test Indices", "Some more test indices", "moretest");
        api.streams().createStream("New Stream", newIndexSetId);
        api.streams().createStream("New Stream 2", defaultIndexSetId);
        api.streams().createStream("New Stream 3", newIndexSetId2);

        api.streams().createStream("sorttest: aaaaa", defaultIndexSetId);
        api.streams().createStream("sorttest: ZZZZZZ", defaultIndexSetId, false);
        api.streams().createStream("sorttest: 12345", defaultIndexSetId, false);
    }

    @ContainerMatrixTest
    void sortByIndexSetTitle() {
        paginatedByFieldWithOrder("New", "title", "asc")
                .assertThat()
                .body(ELEMENTS_FIELD_NAME + "*.title", equalTo(List.of("New Stream", "New Stream 2", "New Stream 3")));
        paginatedByFieldWithOrder("New", "title", "desc")
                .assertThat()
                .body(ELEMENTS_FIELD_NAME + "*.title", equalTo(List.of("New Stream 3", "New Stream 2", "New Stream")));
        paginatedByFieldWithOrder("New", "index_set_title", "asc")
                .assertThat()
                .body(ELEMENTS_FIELD_NAME + "*.title", equalTo(List.of("New Stream 2", "New Stream 3", "New Stream")));
        paginatedByFieldWithOrder("New", "index_set_title", "desc")
                .assertThat()
                .body(ELEMENTS_FIELD_NAME + "*.title", equalTo(List.of("New Stream", "New Stream 3", "New Stream 2")));
    }

    @ContainerMatrixTest
    void sortByTitleCaseInsensitive() {
        paginatedByFieldWithOrder("sorttest", "title", "asc")
                .assertThat()
                .body(ELEMENTS_FIELD_NAME + "*.title", equalTo(List.of("sorttest: 12345", "sorttest: aaaaa", "sorttest: ZZZZZZ")));
        paginatedByFieldWithOrder("sorttest", "title", "desc")
                .assertThat()
                .body(ELEMENTS_FIELD_NAME + "*.title", equalTo(List.of("sorttest: ZZZZZZ", "sorttest: aaaaa", "sorttest: 12345")));
    }

    @ContainerMatrixTest
    void sortByStatus() {
        paginatedByFieldWithOrder("sorttest", "disabled", "asc")
                .assertThat()
                .body(ELEMENTS_FIELD_NAME + "*.title", equalTo(List.of("sorttest: aaaaa", "sorttest: ZZZZZZ", "sorttest: 12345")));
        paginatedByFieldWithOrder("sorttest", "disabled", "desc")
                .assertThat()
                .body(ELEMENTS_FIELD_NAME + "*.title", equalTo(List.of("sorttest: ZZZZZZ", "sorttest: 12345", "sorttest: aaaaa")));
    }

    private ValidatableResponse paginatedByFieldWithOrder(String query, String field, String order) {
        return given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .queryParam("query", query)
                .queryParam("sort", field)
                .queryParam("order", order)
                .get(STREAMS_RESOURCE + "/paginated")
                .then()
                .log().ifValidationFails();
    }
}
