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

import org.apache.shiro.authz.Permission;

/**
 * A permission that grants nothing.
 * <p>
 * Used as a fail-closed substitute wherever a permission string cannot be constructed safely, so that a malformed or
 * hostile identifier results in no access instead of an overly broad wildcard permission.
 */
public final class NoPermission implements Permission {
    public static final NoPermission INSTANCE = new NoPermission();

    private NoPermission() {
    }

    @Override
    public boolean implies(Permission p) {
        return false;
    }

    @Override
    public String toString() {
        return "<no permission>";
    }
}
