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
import com.google.common.collect.ImmutableSetMultimap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@Singleton
public class CapabilityRegistry {
    private final ImmutableMap<Capability, CapabilityDescriptor> capabilities;

    @Inject
    public CapabilityRegistry(GRNRegistry grnRegistry, Set<PluginPermissions> pluginPermissions) {
        final var viewPermissionBuilder = ImmutableSetMultimap.<GRNType, Permission>builder();
        final var managePermissionBuilder = ImmutableSetMultimap.<GRNType, Permission>builder();
        final var ownPermissionBuilder = ImmutableSetMultimap.<GRNType, Permission>builder();

        pluginPermissions.stream()
                .flatMap(permissions -> permissions.permissions().stream())
                .forEach(permission -> permission.grnTypeCapabilities().forEach((grnType, capability) -> {
                    switch (capability) {
                        case VIEW -> viewPermissionBuilder.put(grnType, permission);
                        case MANAGE -> managePermissionBuilder.put(grnType, permission);
                        case OWN -> ownPermissionBuilder.put(grnType, permission);
                    }
                }));

        // The own permission is a special case that applies to all GRN types.
        grnRegistry.forEach(grnType -> ownPermissionBuilder.put(grnType, Permission.ENTITY_OWN));

        final var viewPermissions = viewPermissionBuilder.build();
        // Managers can also view everything, so we add the view permissions to the manage permissions.
        final var managePermissions = managePermissionBuilder.putAll(viewPermissions).build();
        // Owners can also manage everything, so we add the manage permissions to the own permissions.
        final var ownPermissions = ownPermissionBuilder.putAll(managePermissions).build();

        this.capabilities = ImmutableMap.<Capability, CapabilityDescriptor>builder()
                .put(Capability.VIEW, CapabilityDescriptor.builder()
                        .capability(Capability.VIEW)
                        .title("Viewer")
                        .permissions(viewPermissions)
                        .build()
                )
                .put(Capability.MANAGE, CapabilityDescriptor.builder()
                        .capability(Capability.MANAGE)
                        .title("Manager")
                        .permissions(managePermissions)
                        .build()
                )
                .put(Capability.OWN, CapabilityDescriptor.builder()
                        .capability(Capability.OWN)
                        .title("Owner")
                        .permissions(ownPermissions)
                        .build()
                )
                .build();
    }

    public ImmutableSet<CapabilityDescriptor> allSharingCapabilities() {
        return ImmutableSet.of(
                requireNonNull(capabilities.get(Capability.VIEW)),
                requireNonNull(capabilities.get(Capability.MANAGE)),
                requireNonNull(capabilities.get(Capability.OWN))
        );
    }

    public Optional<CapabilityDescriptor> get(Capability capability) {
        return Optional.ofNullable(capabilities.get(capability));
    }

    public Set<Permission> getPermissions(Capability capability, GRNType grnType) {
        return requireNonNull(capabilities.get(capability)).permissions().get(grnType);
    }
}
