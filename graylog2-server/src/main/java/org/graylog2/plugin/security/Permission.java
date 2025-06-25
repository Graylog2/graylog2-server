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

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

/**
 * Represents a permission in the Graylog system.
 * <p>
 * Permissions are used to control access to various resources and actions within Graylog.
 * They can be associated with GRN types and capabilities, allowing for fine-grained access control.
 */
public interface Permission {
    /**
     * Special permission that is used to check if a user has ownership of an entity.
     */
    EntityOwnPermission ENTITY_OWN = EntityOwnPermission.create();

    /**
     * Returns the permission string, e.g. "users:edit", "stream:read", etc.
     * <p>
     * This can be use in Shiro permission checks, e.g. {@code isPermitted("users:edit", userId)'}.
     *
     * @return the permission string
     */
    String permission();

    /**
     * Returns a human-readable description of the permission.
     *
     * @return the description of the permission
     */
    String description();

    /**
     * Returns a map of GRN types to capabilities that this permission applies to.
     * <p>
     * This allows for specifying which GRN types this permission is applicable to and what capabilities are granted.
     *
     * @return an immutable map of GRN types to capabilities
     */
    ImmutableMap<GRNType, Capability> grnTypeCapabilities();

    /**
     * Converts this permission to a Shiro permission object using the specified GRN target.
     *
     * @param target the GRN target for which the permission is being checked
     * @return a Shiro permission object representing this permission
     */
    org.apache.shiro.authz.Permission toShiroPermission(GRN target);

    /**
     * Creates a new permission with the specified permission string and description.
     * <p>
     * This is a convenience method to create a permission without GRN type capabilities.
     *
     * @param permission  the permission string, e.g. "users:edit"
     * @param description a human-readable description of the permission
     * @return a new Permission instance
     */
    static Permission create(String permission, String description) {
        return StringPermission.create(permission, description, ImmutableMap.of());
    }

    /**
     * Creates a new permission with the specified permission string, description, and GRN type capabilities.
     * <p>
     * This allows for specifying which GRN types and capabilities this permission applies to.
     *
     * @param permission          the permission string, e.g. "users:edit"
     * @param description         a human-readable description of the permission
     * @param grnTypeCapabilities a variable number of GRN type capabilities
     * @return a new Permission instance
     */
    static Permission create(String permission, String description, GRNTypeCapability... grnTypeCapabilities) {
        return StringPermission.create(
                permission,
                description,
                Arrays.stream(grnTypeCapabilities)
                        .collect(ImmutableMap.toImmutableMap(GRNTypeCapability::grnType, GRNTypeCapability::capability))
        );
    }

    /**
     * Creates a new {@link Capability#VIEW} capability mapping for the given GRN type.
     * <p>
     * Used for specifying which GRN types and capabilities a permission applies to.
     *
     * @param grnType the GRN type
     * @return a GRNTypeCapability instance
     */
    static GRNTypeCapability addToViewCapabilityFor(GRNType grnType) {
        return new GRNTypeCapability(grnType, Capability.VIEW);
    }

    /**
     * Creates a new {@link Capability#MANAGE} capability mapping for the given GRN type.
     * <p>
     * Used for specifying which GRN types and capabilities a permission applies to.
     *
     * @param grnType the GRN type
     * @return a GRNTypeCapability instance
     */
    static GRNTypeCapability addToManageCapabilityFor(GRNType grnType) {
        return new GRNTypeCapability(grnType, Capability.MANAGE);
    }

    /**
     * Creates a new {@link Capability#OWN} capability mapping for the given GRN type.
     * <p>
     * Used for specifying which GRN types and capabilities a permission applies to.
     *
     * @param grnType the GRN type
     * @return a GRNTypeCapability instance
     */
    static GRNTypeCapability addToOwnCapabilityFor(GRNType grnType) {
        return new GRNTypeCapability(grnType, Capability.OWN);
    }

    /**
     * Represents a mapping of a GRN type to a capability.
     */
    record GRNTypeCapability(GRNType grnType, Capability capability) {
        public GRNTypeCapability {
            requireNonNull(grnType, "grnType must not be null");
            requireNonNull(capability, "capability must not be null");
        }
    }
}
