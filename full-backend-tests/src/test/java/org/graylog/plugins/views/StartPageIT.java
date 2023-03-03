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
import org.graylog.testing.completebackend.apis.Streams;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;

@ContainerMatrixTestsConfiguration
public class StartPageIT {
    private final GraylogApis api;

    private static final Users.User user = new Users.User(
            "john.doe2",
            "asdfgh",
            "John",
            "Doe",
            "john.doe2@example.com",
            false,
            30_000,
            "Europe/Vienna",
            Collections.emptyList(),
            List.of(RestPermissions.DASHBOARDS_CREATE)
    );

    public StartPageIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeAll
    public void init() {
        api.users().createUser(this.user);
    }

    @ContainerMatrixTest
    void testCreateLastOpenedItem() {
        api.postWithResource("/views/search", user,"org/graylog/plugins/views/startpage-save-search-request.json", 201);
        api.postWithResource("/views", user,"org/graylog/plugins/views/startpage-views-request.json", 200);

        var validatableResponse = api.get("/views", user, Map.of(), 200);
        var id = validatableResponse.extract().jsonPath().get("views[0].id");

        api.get("/views/" + id, user, Map.of(), 200);
        validatableResponse = api.get("/startpage/lastOpened", user, Map.of(), 200);
        validatableResponse.assertThat().body("lastOpened[0].id", equalTo(id));
    }


    @ContainerMatrixTest
    void testCreateRecentActivity() {
        final String defaultIndexSetId = api.indices().defaultIndexSetId();
        var stream1Id = api.streams().createStream("Stream #1", defaultIndexSetId, new Streams.StreamRule(StreamRuleType.EXACT.toInteger(), "stream1", "target_stream", false));

        var validatableResponse = api.get("/startpage/recentActivity", Users.LOCAL_ADMIN, Map.of(), 200);
        validatableResponse.assertThat().body("recentActivity[0].item_id", equalTo(stream1Id));
    }
}
