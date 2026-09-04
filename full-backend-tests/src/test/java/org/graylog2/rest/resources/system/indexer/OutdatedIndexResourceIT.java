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
package org.graylog2.rest.resources.system.indexer;

import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.CLASS)
public class OutdatedIndexResourceIT {

    private static final String BULK_DELETE_URL = "/system/indexer/indices/outdated/bulk_delete";

    private static GraylogApis api;
    private static Users.User unprivilegedUser;
    private static Users.User deleteUser;

    @BeforeAll
    static void setUp(GraylogApis graylogApis) {
        api = graylogApis;

        unprivilegedUser = new Users.User("outdated.noaccess", "password123!", "Outdated", "NoAccess",
                "outdated.noaccess@graylog", false, 300_000, "UTC", List.of(), List.of());
        api.users().createUser(unprivilegedUser);

        deleteUser = new Users.User("outdated.deleter", "password123!", "Outdated", "Deleter",
                "outdated.deleter@graylog", false, 300_000, "UTC", List.of(), List.of(RestPermissions.INDICES_DELETE));
        api.users().createUser(deleteUser);
    }

    @AfterAll
    static void tearDown() {
        api.users().deleteUser(unprivilegedUser.username());
        api.users().deleteUser(deleteUser.username());
    }

    @FullBackendTest
    void bulkDeleteRejectsCallerWithoutAnyDeletePermission() {
        given()
                .spec(api.forUser(unprivilegedUser).requestSpecification())
                .when()
                .body(new BulkOperationRequest(List.of("graylog_0", "definitely_not_a_real_index_9f3a")))
                .post(BULK_DELETE_URL)
                .then()
                .statusCode(403);
    }

    @FullBackendTest
    void bulkDeleteReportsFailuresForUnknownIndicesWhenPermitted() {
        given()
                .spec(api.forUser(deleteUser).requestSpecification())
                .when()
                .body(new BulkOperationRequest(List.of("graylog_0", "definitely_not_a_real_index_9f3a")))
                .post(BULK_DELETE_URL)
                .then()
                .statusCode(200)
                .body("successfully_performed", equalTo(0))
                .body("failures.entity_id", containsInAnyOrder("graylog_0", "definitely_not_a_real_index_9f3a"));
    }
}
