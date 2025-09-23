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
import org.graylog.testing.containermatrix.annotations.FullBackendTest;
import org.graylog.testing.containermatrix.annotations.GraylogBackendConfiguration;
import org.junit.jupiter.api.BeforeAll;

import static org.hamcrest.core.IsEqual.equalTo;

@GraylogBackendConfiguration
public class ViewsResourceIT {
    private static GraylogApis api;

    @BeforeAll
    static void init(GraylogApis graylogApis) {
        api = graylogApis;
    }

    @FullBackendTest
    void testEmptyBody() {
        api.post("/views", 400)
                 .assertThat().body("message[0]", equalTo("View is mandatory"));
    }

    @FullBackendTest
    void testCreateViewRequestWithoutPersistedSearch() {
        api.postWithResource("/views", "org/graylog/plugins/views/views-request.json", 400);
    }

    @FullBackendTest
    void testCreateSearchPersistView() {
        api.postWithResource("/views/search", "org/graylog/plugins/views/save-search-request.json", 201);
        api.postWithResource("/views", "org/graylog/plugins/views/views-request.json", 200);
    }

    @FullBackendTest
    void testInvalidSearchType() {
        api.postWithResource("/views/search", "org/graylog/plugins/views/save-search-request-invalid.json", 201);
        api.postWithResource("/views", "org/graylog/plugins/views/views-request-invalid-search-type.json", 400)
                .assertThat()
                .body("message", equalTo("Search types do not correspond to view/search types, missing searches [967d2217-fd99-48a6-b829-5acdab906808]; search types: [967d2217-fd99-48a6-b829-5acdab906807]; state types: [967d2217-fd99-48a6-b829-5acdab906808]"));
    }
}
