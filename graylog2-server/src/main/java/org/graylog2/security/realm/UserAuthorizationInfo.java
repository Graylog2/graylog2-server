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
package org.graylog2.security.realm;

import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.graylog2.plugin.database.users.User;

import java.util.Set;

public class UserAuthorizationInfo extends SimpleAuthorizationInfo {
    private final User user;

    public UserAuthorizationInfo(User user) {
        super();
        this.user = user;
    }

    public UserAuthorizationInfo(Set<String> roles, User user) {
        super(roles);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
