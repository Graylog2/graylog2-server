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

import org.graylog.grn.GRNType;
import org.graylog.security.Capability;

import static java.util.Objects.requireNonNull;

public interface Permission {
    String permission();

    String description();

    static Permission create(String permission, String description) {
        return LegacyPermission.create(permission, description);
    }

    static Permission create(String permission, String description, GRNTypeCapability... grnTypeCapabilities) {
        return LegacyPermission.create(permission, description);
    }

    static GRNTypeCapability viewCapability(GRNType grnType) {
        return new GRNTypeCapability(grnType, Capability.VIEW);
    }

    static GRNTypeCapability manageCapability(GRNType grnType) {
        return new GRNTypeCapability(grnType, Capability.MANAGE);
    }

    static GRNTypeCapability ownCapability(GRNType grnType) {
        return new GRNTypeCapability(grnType, Capability.OWN);
    }

    record GRNTypeCapability(GRNType grnType, Capability capability) {
        public GRNTypeCapability {
            requireNonNull(grnType, "grnType must not be null");
            requireNonNull(capability, "capability must not be null");
        }
    }
}
