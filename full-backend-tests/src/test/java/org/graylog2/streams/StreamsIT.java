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
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.graylog2.rest.models.tools.responses.PageListResponse.ELEMENTS_FIELD_NAME;
import static org.hamcrest.Matchers.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS)
public class StreamsIT {
    private static final String STREAMS_RESOURCE = "/streams";

    private final GraylogApis api;
    private final List<String> createdStreamsIds;

    public StreamsIT(GraylogApis api) {
        this.api = api;
        this.createdStreamsIds = new ArrayList<>();
    }

    @BeforeAll
    void beforeAll() {
        final String defaultIndexSetId = api.indices().defaultIndexSetId();
        final String newIndexSetId = api.indices().createIndexSet("Test Indices", "Some test indices", "streamstest");
        final String newIndexSetId2 = api.indices().createIndexSet("More Test Indices", "Some more test indices", "moretest");
        createdStreamsIds.add(api.streams().createStream("New Stream", newIndexSetId));
        createdStreamsIds.add(api.streams().createStream("New Stream 2", defaultIndexSetId));
        createdStreamsIds.add(api.streams().createStream("New Stream 3", newIndexSetId2));

        createdStreamsIds.add(api.streams().createStream("sorttest: aaaaa", defaultIndexSetId));
        createdStreamsIds.add(api.streams().createStream("sorttest: ZZZZZZ", defaultIndexSetId, false));
        createdStreamsIds.add(api.streams().createStream("sorttest: 12345", defaultIndexSetId, false));
    }

    @ContainerMatrixTest
    void bulkPauseAndResumeWorksCorrectly() {
        //Testing pause and resume in the same test, as other test checks sorting by status, so I want to bring back original situation

        //picking "New Stream" and "sorttest: aaaaa" for test, adding one wrong ID
        final List<String> bulkEntityIds = List.of(
                createdStreamsIds.get(0),
                createdStreamsIds.get(3),
                "wrong ID!");

        //test bulk pause
        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .body(new BulkOperationRequest(bulkEntityIds))
                .post(STREAMS_RESOURCE + "/bulk_pause")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("successfully_performed", equalTo(2))
                .body("failures[0].entity_id", equalTo("wrong ID!"));

        api.streams().getStream(createdStreamsIds.get(0)).body("disabled", equalTo(true));
        api.streams().getStream(createdStreamsIds.get(3)).body("disabled", equalTo(true));

        //test bulk resume
        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .body(new BulkOperationRequest(bulkEntityIds))
                .post(STREAMS_RESOURCE + "/bulk_resume")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("successfully_performed", equalTo(2))
                .body("failures[0].entity_id", equalTo("wrong ID!"));

        api.streams().getStream(createdStreamsIds.get(0)).body("disabled", equalTo(false));
        api.streams().getStream(createdStreamsIds.get(3)).body("disabled", equalTo(false));
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
