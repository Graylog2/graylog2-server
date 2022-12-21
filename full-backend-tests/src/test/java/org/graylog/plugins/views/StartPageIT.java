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

import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collections;

@ContainerMatrixTestsConfiguration
public class StartPageIT {
    private final RequestSpecification requestSpec;
    private final GraylogApis api;

    public StartPageIT(RequestSpecification requestSpec, GraylogApis apis) {
        this.requestSpec = requestSpec;
        this.api = apis;
    }

    @BeforeAll
    public void init() {
        final JsonPath user = api.users().createUser(new Users.User(
                "john.doe2",
                "asdfgh",
                "John",
                "Doe",
                "john.doe2@example.com",
                false,
                30_000,
                "Europe/Vienna",
                Collections.emptyList(),
                Collections.emptyList()
        ));
    }

    @ContainerMatrixTest
    void testCreateRecentActivity() {

    }

    @ContainerMatrixTest
    void testCreateLastOpened() {

    }
}
