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
package org.graylog2.plugin.security;

import com.google.common.collect.ImmutableMap;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNType;
import org.graylog.security.Capability;
import org.graylog.security.permissions.GRNPermission;

public record EntityOwnPermission(String permission,
                                  String description,
                                  ImmutableMap<GRNType, Capability> grnTypeCapabilities) implements Permission {
    public static EntityOwnPermission create() {
        // Use a PermissionWithGRNTypes detour to validate the permission string.
        final var permission = StringPermission.create("entity:own", "Entity ownership permission.", ImmutableMap.of());
        return new EntityOwnPermission(permission.permission(), permission.description(), permission.grnTypeCapabilities());
    }

    @Override
    public org.apache.shiro.authz.Permission toShiroPermission(GRN target) {
        return GRNPermission.create(permission, target);
    }
}
