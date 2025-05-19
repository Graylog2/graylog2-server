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
package org.graylog.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog2.shared.security.RestPermissions;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.Set;

public interface BuiltinCapabilities {
    ImmutableMap<Capability, CapabilityDescriptor> capabilities();

    default ImmutableSet<CapabilityDescriptor> allSharingCapabilities() {
        return ImmutableSet.of(
                capabilities().get(Capability.VIEW),
                capabilities().get(Capability.MANAGE),
                capabilities().get(Capability.OWN)
        );
    }

    default Optional<CapabilityDescriptor> get(Capability capability) {
        return Optional.ofNullable(capabilities().get(capability));
    }
}
