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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.plugin.database.users.User;
import org.mockito.Mockito;

import static org.graylog2.shared.security.RestPermissions.DASHBOARDS_READ;
import static org.graylog2.shared.security.RestPermissions.STREAMS_READ;

public class TestSearchUser {

    final ImmutableMap.Builder<String, Boolean> permissions;

    private TestSearchUser() {
        permissions = ImmutableMap.builder();
    }

    public static TestSearchUser builder() {
        return new TestSearchUser();
    }

    public TestSearchUser allowStream(String streamId) {
        this.permissions.put(STREAMS_READ + ":" + streamId, true);
        return this;
    }

    public TestSearchUser allowDashboard(String id) {
        this.permissions.put(DASHBOARDS_READ + ":" + id, true);
        return this;
    }

    public TestSearchUser denyStream(String streamId) {
        this.permissions.put(STREAMS_READ + ":" + streamId, false);
        return this;
    }

    public TestSearchUser denyDashboard(String id) {
        this.permissions.put(DASHBOARDS_READ + ":" + id, false);
        return this;
    }

    public SearchUser build() {
        final ImmutableMap<String, Boolean> build = permissions.build();
        return new SearchUser(Mockito.mock(User.class), s -> build.getOrDefault(s, false), (u, v) -> build.getOrDefault(u + ":" + v, false));
    }
}
