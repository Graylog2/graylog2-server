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

package org.graylog2.contentpacks.model;

import org.apache.shiro.authz.annotation.Logical;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record EntityPermissions(List<String> permissions, Logical operator) {

    public EntityPermissions(List<String> permissions) {
        this(permissions, Logical.AND);
    }

    public static Optional<EntityPermissions> of(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return Optional.empty();
        } else {
            return Optional.of(new EntityPermissions(Arrays.asList(permissions)));
        }
    }
}
