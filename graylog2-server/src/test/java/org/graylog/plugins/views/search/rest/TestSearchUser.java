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
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

import static org.graylog2.shared.security.RestPermissions.DASHBOARDS_READ;
import static org.graylog2.shared.security.RestPermissions.STREAMS_READ;
import static org.mockito.Mockito.when;

public class TestSearchUser {

    final ImmutableMap.Builder<String, Boolean> permissions;
    final ImmutableSet.Builder<String> knownStreams;
    private User user;

    private TestSearchUser() {
        permissions = ImmutableMap.builder();
        knownStreams = ImmutableSet.builder();
    }

    public static TestSearchUser builder() {
        return new TestSearchUser();
    }

    public TestSearchUser allowStream(String streamId) {
        this.permissions.put(STREAMS_READ + ":" + streamId, true);
        this.knownStreams.add(streamId);
        return this;
    }

    public TestSearchUser allowDashboard(String id) {
        this.permissions.put(DASHBOARDS_READ + ":" + id, true);
        return this;
    }

    public TestSearchUser denyStream(String streamId) {
        this.permissions.put(STREAMS_READ + ":" + streamId, false);
        this.knownStreams.add(streamId);
        return this;
    }

    public TestSearchUser denyDashboard(String id) {
        this.permissions.put(DASHBOARDS_READ + ":" + id, false);
        return this;
    }

    public TestSearchUser allowView(String id) {
        this.permissions.put(ViewsRestPermissions.VIEW_READ + ":" + id, true);
        return this;
    }


    public TestSearchUser allowEditView(String id) {
        this.permissions.put(ViewsRestPermissions.VIEW_EDIT + ":" + id, true);
        return this;
    }

    public TestSearchUser denyView(String id) {
        this.permissions.put(ViewsRestPermissions.VIEW_READ + ":" + id, false);
        return this;
    }


    public TestSearchUser denyEditView(String id) {
        this.permissions.put(ViewsRestPermissions.VIEW_EDIT + ":" + id, false);
        return this;
    }


    public TestSearchUser allowDeleteView(String viewId) {
        this.permissions.put(ViewsRestPermissions.VIEW_DELETE  + ":" + viewId, true);
        return this;
    }

    public TestSearchUser canCreateDashboards(boolean canCreateDashboards) {
        this.permissions.put(RestPermissions.DASHBOARDS_CREATE, canCreateDashboards);
        return this;
    }

    public TestSearchUser withUser(User user) {
        this.user = user;
        return this;
    }

    public TestSearchUser withUser(Consumer<TestUser> user) {
        final TestUser testUser = TestUser.builder();
        user.accept(testUser);
        this.user = testUser.build();
        return this;
    }

    public SearchUser build() {
        final ImmutableMap<String, Boolean> permissions = this.permissions.build();
        final ImmutableSet<String> knownStreamIDs = knownStreams.build();

        return new SearchUser(
                Optional.ofNullable(user).orElseGet(() -> Mockito.mock(User.class)),
                permission -> verifyPermission(permissions, permission),
                (permission, entityid) -> verifyPermission(permissions, permission, entityid),
                new PermittedStreams(knownStreamIDs::stream),
                new HashMap<>());
    }

    private Boolean verifyPermission(ImmutableMap<String, Boolean> permissions, String permission, String entityId) {
        return verifyPermission(permissions, permission + ":" + entityId);
    }

    private Boolean verifyPermission(ImmutableMap<String, Boolean> permissions, String permission) {
        return permissions.getOrDefault(permission, false);
    }
}
