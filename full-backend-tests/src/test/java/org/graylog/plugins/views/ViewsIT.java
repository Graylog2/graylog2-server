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

import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

import static org.hamcrest.core.StringContains.containsString;

@ContainerMatrixTestsConfiguration
public class ViewsIT {
    private final GraylogApis api;

    public ViewsIT(GraylogApis apis) {
        this.api = apis;
    }

    @BeforeAll
    public void importMongoFixtures() {
        this.api.backend().importMongoDBFixture("mongodb-stored-views-for-issue15086.json", ViewsIT.class);
    }

    @ContainerMatrixTest
    void testIssue15086Dashboard() {
        api.get("/views/63e0f94c17263921e7fefeb3", Map.of(), 200)
                .assertThat().body(containsString("org.graylog2.decorators.FormatStringDecorator"));
    }

    @ContainerMatrixTest
    void testIssue15086Search() {
        api.get("/views/643e60873c985e899977ba1c", Map.of(), 200)
                .assertThat().body(containsString("org.graylog2.decorators.FormatStringDecorator"));
    }
}
