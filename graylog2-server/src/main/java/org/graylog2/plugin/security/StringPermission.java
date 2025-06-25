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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNType;
import org.graylog.security.Capability;
import org.graylog.security.permissions.CaseSensitiveWildcardPermission;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

// This record has package-private visibility to prevent usage outside the security package.
record StringPermission(String object,
                        String action,
                        String description,
                        ImmutableMap<GRNType, Capability> grnTypeCapabilities) implements Permission {
    public StringPermission {
        // This is a special case for a legacy permission that was not following the object:action format
        if (!("streams".equals(object) && "read:datastream:gl-security-investigations-metrics".equals(action))) {
            validatePart(object, "object");
            validatePart(action, "action");
        }
        requireNonNull(description, "description must not be null");
        requireNonNull(grnTypeCapabilities, "grnTypeCapabilities must not be null");
    }

    private static void validatePart(String value, String name) {
        requireNonBlank(value, name + " must not be blank");

        if (!value.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException(name + " can only contain alphanumeric characters and underscores, but was: " + value);
        }
    }

    @Override
    public String permission() {
        return object + ":" + action;
    }

    @Override
    public org.apache.shiro.authz.Permission toShiroPermission(GRN target) {
        return new CaseSensitiveWildcardPermission(permission() + ":" + target.entity());
    }

    public static Permission create(@Nonnull String permission, @Nullable String description, ImmutableMap<GRNType, Capability> grnTypeCapabilities) {
        requireNonBlank(permission, "permission must not be blank");

        final var parts = permission.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("permission must be in the format 'object:action', but was: " + permission);
        }

        return new StringPermission(
                parts[0],
                parts[1],
                requireNonNullElse(description, "").trim(),
                grnTypeCapabilities
        );
    }
}
