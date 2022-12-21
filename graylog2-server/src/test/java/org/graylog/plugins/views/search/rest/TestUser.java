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

import org.graylog2.plugin.database.users.User;
import org.mockito.Mockito;

public class TestUser {

    private String username;

    private String id;

    private boolean isLocalAdmin = false;

    public static TestUser builder() {
        return new TestUser();
    }

    public TestUser withUsername(final String username) {
        this.username = username;
        return this;
    }

    public TestUser isLocalAdmin(final boolean isLocalAdmin) {
        this.isLocalAdmin = isLocalAdmin;
        return this;
    }

    public TestUser withId(final String id) {
        this.id = id;
        return this;
    }

    public User build() {
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getName()).thenReturn(username);
        Mockito.when(user.getId()).thenReturn(id);
        Mockito.when(user.isLocalAdmin()).thenReturn(isLocalAdmin);
        return user;
    }
}
