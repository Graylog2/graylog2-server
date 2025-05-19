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
import jakarta.inject.Singleton;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog2.shared.security.RestPermissions;

import java.util.Optional;
import java.util.Set;

@Singleton
public class DefaultBuiltinCapabilities implements BuiltinCapabilities{
    protected static ImmutableMap<Capability, CapabilityDescriptor> CAPABILITIES;

    @Override
    public ImmutableMap<Capability, CapabilityDescriptor> capabilities() {
        return CAPABILITIES;
    }

    protected Set<String> readPermissions() {
        return Set.of(
                RestPermissions.STREAMS_READ,
                RestPermissions.STREAM_OUTPUTS_READ,
                RestPermissions.DASHBOARDS_READ,
                ViewsRestPermissions.VIEW_READ,
                RestPermissions.EVENT_DEFINITIONS_READ,
                RestPermissions.EVENT_NOTIFICATIONS_READ,
                RestPermissions.OUTPUTS_READ,
                RestPermissions.SEARCH_FILTERS_READ
        );
    }

    protected Set<String> editPermissions() {
        return Set.of(
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
    }

    protected Set<String> deletePermissions() {
        return Set.of(
                RestPermissions.STREAM_OUTPUTS_DELETE,
                ViewsRestPermissions.VIEW_DELETE,
                RestPermissions.EVENT_DEFINITIONS_DELETE,
                RestPermissions.EVENT_NOTIFICATIONS_DELETE,
                RestPermissions.OUTPUTS_TERMINATE,
                RestPermissions.SEARCH_FILTERS_DELETE
        );
    }

    public DefaultBuiltinCapabilities() {
        CAPABILITIES = ImmutableMap.<Capability, CapabilityDescriptor>builder()
                .put(Capability.VIEW, CapabilityDescriptor.create(
                        Capability.VIEW,
                        "Viewer",
                        readPermissions()
                ))
                .put(Capability.MANAGE, CapabilityDescriptor.create(
                        Capability.MANAGE,
                        "Manager",
                        ImmutableSet.<String>builder()
                                .addAll(readPermissions())
                                .addAll(editPermissions())
                                .build()
                ))
                .put(Capability.OWN, CapabilityDescriptor.create(
                                Capability.OWN,
                                "Owner",
                                ImmutableSet.<String>builder()
                                        .add(RestPermissions.ENTITY_OWN)
                                        .addAll(readPermissions())
                                        .addAll(editPermissions())
                                        .addAll(deletePermissions())
                                        .build()
                        )
                )
                .build();
    }
}
