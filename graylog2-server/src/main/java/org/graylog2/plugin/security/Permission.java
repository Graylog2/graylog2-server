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

import org.graylog.grn.GRN;
import org.graylog.grn.GRNType;
import org.graylog.security.Capability;
import org.graylog.security.permissions.GRNPermission;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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
    Permission ENTITY_OWN = new Permission() {
        private final static String permission = "entity:own";

        @Override
        public String permission() {
            return permission;
        }

        @Override
        public String description() {
            return "Entity ownership permission.";
        }

        @Override
        public Map<GRNType, Capability> grnTypeCapabilities() {
            return Map.of();
        }

        @Override
        public org.apache.shiro.authz.Permission toShiroPermission(GRN target) {
            return GRNPermission.create(permission, target);
        }

        @Override
        public Permission withCapabilityFor(GRNType grnType, Capability capability) {
            throw new UnsupportedOperationException("ENTITY_OWN permission does not support capabilities.");
        }
    };

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
     * @return a map of GRN types to capabilities
     */
    Map<GRNType, Capability> grnTypeCapabilities();

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
        return DomainActionPermission.create(permission, description, Map.of());
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
        return DomainActionPermission.create(
                permission,
                description,
                Arrays.stream(grnTypeCapabilities)
                        .collect(Collectors.toUnmodifiableMap(GRNTypeCapability::grnType, GRNTypeCapability::capability))
        );
    }

    /**
     * Adds the specified GRN type and {@link Capability#VIEW} capability to the permission.
     *
     * @param grnType the GRN type to associate with this permission
     * @return a new Permission instance with the view capability for the specified GRN type
     */
    default Permission withViewCapabilityFor(GRNType grnType) {
        return withCapabilityFor(grnType, Capability.VIEW);
    }

    /**
     * Adds the specified GRN type and {@link Capability#MANAGE} capability to the permission.
     *
     * @param grnType the GRN type to associate with this permission
     * @return a new Permission instance with the manage capability for the specified GRN type
     */
    default Permission withManageCapabilityFor(GRNType grnType) {
        return withCapabilityFor(grnType, Capability.MANAGE);
    }

    /**
     * Adds the specified GRN type and {@link Capability#OWN} capability to the permission.
     *
     * @param grnType the GRN type to associate with this permission
     * @return a new Permission instance with the own capability for the specified GRN type
     */
    default Permission withOwnCapabilityFor(GRNType grnType) {
        return withCapabilityFor(grnType, Capability.OWN);
    }

    /**
     * Creates a new permission with the specified GRN type and capability.
     *
     * @param grnType    the GRN type to associate with this permission
     * @param capability the capability to associate with the GRN type
     * @return a new Permission instance with the updated GRN type capabilities
     */
    Permission withCapabilityFor(GRNType grnType, Capability capability);

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
