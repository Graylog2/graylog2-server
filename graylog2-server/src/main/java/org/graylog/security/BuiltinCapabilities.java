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

@Singleton
public class BuiltinCapabilities {
    private static ImmutableMap<Capability, CapabilityDescriptor> CAPABILITIES;

    @Inject
    public BuiltinCapabilities(Set<CapabilityPermissions> capabilities) {
        final ImmutableSet.Builder<String> readPermissionBuilder = ImmutableSet.builder();
        final ImmutableSet.Builder<String> editPermissionBuilder = ImmutableSet.builder();
        final ImmutableSet.Builder<String> deletePermissionBuilder = ImmutableSet.builder();

        capabilities.stream().forEach(
                permissions -> {
                    readPermissionBuilder.addAll(permissions.readPermissions());
                    editPermissionBuilder.addAll(permissions.editPermissions());
                    deletePermissionBuilder.addAll(permissions.deletePermissions());
                }
        );

        final ImmutableSet<String> readPermissions = readPermissionBuilder.build();
        final ImmutableSet<String> editPermissions = editPermissionBuilder.build();
        final ImmutableSet<String> deletePermissions = deletePermissionBuilder.build();

        CAPABILITIES = ImmutableMap.<Capability, CapabilityDescriptor>builder()
                .put(Capability.VIEW, CapabilityDescriptor.create(
                        Capability.VIEW,
                        "Viewer",
                        readPermissions
                ))
                .put(Capability.MANAGE, CapabilityDescriptor.create(
                        Capability.MANAGE,
                        "Manager",
                        ImmutableSet.<String>builder()
                                .addAll(readPermissions)
                                .addAll(editPermissions)
                                .build()
                ))
                .put(Capability.OWN, CapabilityDescriptor.create(
                                Capability.OWN,
                                "Owner",
                                ImmutableSet.<String>builder()
                                        .add(RestPermissions.ENTITY_OWN)
                                        .addAll(readPermissions)
                                        .addAll(editPermissions)
                                        .addAll(deletePermissions)
                                        .build()
                        )
                )
                .build();
    }

    public ImmutableSet<CapabilityDescriptor> allSharingCapabilities() {
        return ImmutableSet.of(
                CAPABILITIES.get(Capability.VIEW),
                CAPABILITIES.get(Capability.MANAGE),
                CAPABILITIES.get(Capability.OWN)
        );
    }

    public Optional<CapabilityDescriptor> get(Capability capability) {
        return Optional.ofNullable(CAPABILITIES.get(capability));
    }
}
