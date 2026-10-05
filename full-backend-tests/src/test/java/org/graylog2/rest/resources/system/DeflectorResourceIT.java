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
package org.graylog2.rest.resources.system;

import com.github.rholder.retry.RetryException;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.CLASS)
public class DeflectorResourceIT {

    private static GraylogApis api;
    private final List<String> createdIndexSetIds = new ArrayList<>();

    @BeforeAll
    void beforeAll(GraylogApis graylogApis) throws ExecutionException, RetryException {
        api = graylogApis;
        createdIndexSetIds.add(api.indices().createIndexSet("Deflector Test 1", "Some test indices", "deflectortest1"));
        createdIndexSetIds.add(api.indices().createIndexSet("Deflector Test 2", "Some more test indices", "deflectortest2"));
    }

    @AfterAll
    void afterAll() {
        createdIndexSetIds.forEach(indexSetId -> api.indices().deleteIndexSet(indexSetId, true));
    }

    @FullBackendTest
    void bulkCycleRotatesIndexSetsAndReportsFailureForUnknownId() {
        final String indexSetId1 = createdIndexSetIds.get(0);
        final String indexSetId2 = createdIndexSetIds.get(1);

        final String targetBeforeCycle1 = api.indices().getDeflectorIndex(indexSetId1);
        final String targetBeforeCycle2 = api.indices().getDeflectorIndex(indexSetId2);

        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .body(new BulkOperationRequest(List.of(indexSetId1, indexSetId2, "wrong ID!")))
                .post("/system/deflector/bulk_cycle")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("successfully_performed", equalTo(2))
                .body("failures[0].entity_id", equalTo("wrong ID!"));

        assertThat(api.indices().getDeflectorIndex(indexSetId1)).isNotEqualTo(targetBeforeCycle1);
        assertThat(api.indices().getDeflectorIndex(indexSetId2)).isNotEqualTo(targetBeforeCycle2);
    }
}
