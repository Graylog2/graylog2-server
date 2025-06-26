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

/**
 * Represents a permission defined by a string in the format "object:action".
 *
 * @param object              the object part of the permission, e.g. "users", "streams"
 * @param action              the action part of the permission, e.g. "edit", "read"
 * @param description         a human-readable description of the permission
 * @param grnTypeCapabilities a map of GRN types to capabilities that this permission applies to
 */
// This record has package-private visibility to prevent usage outside the security package.
record ObjectActionPermission(String object,
                              String action,
                              String description,
                              ImmutableMap<GRNType, Capability> grnTypeCapabilities) implements Permission {
    public ObjectActionPermission {
        // We have some legacy permissions that do not follow the object:action format, but we want to support them for now.
        if (!isLegacyPermission(object, action)) {
            validatePart(object, "object");
            validatePart(action, "action");
        }
        requireNonNull(description, "description must not be null");
        requireNonNull(grnTypeCapabilities, "grnTypeCapabilities must not be null");
    }

    private static boolean isLegacyPermission(String object, String action) {
        // We MUST NOT add more legacy permissions here, this is only for backwards compatibility.
        return switch (object) {
            case "streams" -> "read:datastream:gl-security-investigations-metrics".equals(action);
            case "customization" -> "theme:read".equals(action) || "theme:update".equals(action) ||
                    "notification:read".equals(action) || "notification:update".equals(action);
            default -> false;
        };
    }

    private static void validatePart(String value, String name) {
        requireNonBlank(value, name + " must not be blank");

        if (!value.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException(name + " can only contain alphanumeric characters, hyphens, and underscores, but was: " + value);
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

    @Override
    public Permission withCapabilityFor(GRNType grnType, Capability capability) {
        final var grnTypeCapabilities = ImmutableMap.<GRNType, Capability>builder()
                .putAll(this.grnTypeCapabilities)
                .put(grnType, capability)
                .build();
        return new ObjectActionPermission(object, action, description, grnTypeCapabilities);
    }

    /**
     * Creates a new {@link ObjectActionPermission} instance.
     *
     * @param permission          the permission string in the format "object:action"
     * @param description         a human-readable description of the permission
     * @param grnTypeCapabilities a map of GRN types to capabilities that this permission applies to
     * @return a new Permission instance
     * @throws IllegalArgumentException if the permission string is not in the correct format or is blank
     * @throws NullPointerException     if the permission or grnTypeCapabilities are null
     */
    public static Permission create(@Nonnull String permission, @Nullable String description, ImmutableMap<GRNType, Capability> grnTypeCapabilities) {
        requireNonBlank(permission, "permission must not be blank");

        final var parts = permission.split(":", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("permission must be in the format 'object:action', but was: " + permission);
        }

        return new ObjectActionPermission(
                parts[0],
                parts[1],
                requireNonNullElse(description, "").trim(),
                grnTypeCapabilities
        );
    }
}
