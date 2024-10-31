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

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Streams;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import java.util.Map;

import static org.hamcrest.core.StringContains.containsString;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS)
public class StartPageRecentActivityIT {

    private final GraylogApis api;

    public StartPageRecentActivityIT(GraylogApis api) {
        this.api = api;
    }

    @ContainerMatrixTest
    void testCreateRecentActivity() {
        final String defaultIndexSetId = api.indices().defaultIndexSetId();
        var stream1Id = api.streams().createStream("Stream #1", defaultIndexSetId, Streams.StreamRule.exact("stream1", "target_stream", false));

        var validatableResponse = api.get("/startpage/recentActivity", Users.LOCAL_ADMIN, Map.of(), 200);
        validatableResponse.assertThat().body("recentActivity[0].item_grn", containsString(stream1Id));
    }
}
