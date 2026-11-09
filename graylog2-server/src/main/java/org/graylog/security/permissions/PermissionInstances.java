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
package org.graylog.security.permissions;

import jakarta.annotation.Nullable;

/**
 * Helpers for the "instance" part of a wildcard permission string, i.e. the trailing part of permissions like
 * {@code users:edit:<username>} or {@code streams:read:<stream-id>}.
 */
public final class PermissionInstances {
    private PermissionInstances() {
    }

    /**
     * Checks whether the given value can be safely appended to a permission string as an instance.
     * <p>
     * Shiro's {@link org.apache.shiro.authz.permission.WildcardPermission} parsing assigns special meaning to a few
     * characters: {@code :} separates parts, {@code ,} separates alternatives within a part and {@code *} matches
     * anything. Surrounding whitespace is trimmed. An identifier carrying any of those would therefore widen the
     * permission far beyond the single instance it is meant to address - a user named {@code *} would end up holding
     * {@code users:edit:*} on every other user.
     *
     * @param instance the instance identifier, e.g. a username or entity ID
     * @return true if the value addresses exactly one instance
     */
    public static boolean isSafe(@Nullable String instance) {
        if (instance == null || instance.isEmpty()) {
            return false;
        }
        if (!instance.equals(instance.trim())) {
            return false;
        }
        return instance.chars().noneMatch(c -> c == ':' || c == ',' || c == '*');
    }
}
