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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class BuiltinCapabilities {
    private static ImmutableMap<Capability, CapabilityDescriptor> CAPABILITIES;

    @Inject
    public BuiltinCapabilities() {
        final ImmutableSet<String> readPermissions = ImmutableSet.of(
                RestPermissions.STREAMS_READ,
                RestPermissions.STREAM_OUTPUTS_READ,
                RestPermissions.DASHBOARDS_READ,
                ViewsRestPermissions.VIEW_READ,
                RestPermissions.EVENT_DEFINITIONS_READ,
                RestPermissions.EVENT_NOTIFICATIONS_READ,
                RestPermissions.OUTPUTS_READ,
                RestPermissions.SEARCH_FILTERS_READ
        );

        final ImmutableSet<String> editPermissions = ImmutableSet.of(
                RestPermissions.STREAMS_EDIT,
                RestPermissions.STREAMS_CHANGESTATE,
                RestPermissions.STREAM_OUTPUTS_CREATE,
                RestPermissions.DASHBOARDS_EDIT,
                ViewsRestPermissions.VIEW_EDIT,
                RestPermissions.EVENT_DEFINITIONS_EDIT,
                RestPermissions.EVENT_NOTIFICATIONS_EDIT,
                RestPermissions.OUTPUTS_EDIT,
                RestPermissions.SEARCH_FILTERS_EDIT
        );

        final ImmutableSet<String> deletePermissions = ImmutableSet.of(
                RestPermissions.STREAM_OUTPUTS_DELETE,
                ViewsRestPermissions.VIEW_DELETE,
                RestPermissions.EVENT_DEFINITIONS_DELETE,
                RestPermissions.EVENT_NOTIFICATIONS_DELETE,
                RestPermissions.OUTPUTS_TERMINATE,
                RestPermissions.SEARCH_FILTERS_DELETE
        );


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

    public static ImmutableSet<CapabilityDescriptor> allSharingCapabilities() {
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
