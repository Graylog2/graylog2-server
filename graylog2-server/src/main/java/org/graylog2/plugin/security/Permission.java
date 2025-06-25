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

public interface Permission {
    EntityOwnPermission ENTITY_OWN = EntityOwnPermission.create();

    String permission();

    String description();

    ImmutableMap<GRNType, Capability> grnTypeCapabilities();

    org.apache.shiro.authz.Permission toShiroPermission(GRN target);

    static Permission create(String permission, String description) {
        return PermissionWithGRNTypes.create(permission, description, ImmutableMap.of());
    }

    static Permission create(String permission, String description, GRNTypeCapability... grnTypeCapabilities) {
        return PermissionWithGRNTypes.create(
                permission,
                description,
                Arrays.stream(grnTypeCapabilities)
                        .collect(ImmutableMap.toImmutableMap(GRNTypeCapability::grnType, GRNTypeCapability::capability))
        );
    }

    static GRNTypeCapability addToViewCapabilityFor(GRNType grnType) {
        return new GRNTypeCapability(grnType, Capability.VIEW);
    }

    static GRNTypeCapability addToManageCapabilityFor(GRNType grnType) {
        return new GRNTypeCapability(grnType, Capability.MANAGE);
    }

    static GRNTypeCapability addToOwnCapabilityFor(GRNType grnType) {
        return new GRNTypeCapability(grnType, Capability.OWN);
    }

    record GRNTypeCapability(GRNType grnType, Capability capability) {
        public GRNTypeCapability {
            requireNonNull(grnType, "grnType must not be null");
            requireNonNull(capability, "capability must not be null");
        }
    }
}
