/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class BuiltinCapabilities {
    private static ImmutableMap<Capability, CapabilityDescriptor> CAPABILITIES;

    // TODO: Need to be migrated to roles
    public final static String ROLE_COLLECTION_CREATOR = "grn::::role:54e3deadbeefdeadbeef1001";
    public final static String ROLE_DASHBOARD_CREATOR = "grn::::role:54e3deadbeefdeadbeef1002";
    public final static String ROLE_STREAM_CREATOR = "grn::::role:54e3deadbeefdeadbeef1003";

    @Inject
    public BuiltinCapabilities() {
        CAPABILITIES = ImmutableMap.<Capability, CapabilityDescriptor>builder()
                .put(Capability.VIEW, CapabilityDescriptor.create(
                        Capability.VIEW,
                        "Viewer",
                        ImmutableSet.of(
                                RestPermissions.STREAMS_READ,
                                RestPermissions.DASHBOARDS_READ,
                                RestPermissions.EVENT_DEFINITIONS_READ
                                // TODO: Add missing collection permissions
                        )
                ))
                .put(Capability.MANAGE, CapabilityDescriptor.create(
                        Capability.MANAGE,
                        "Manager",
                        ImmutableSet.of(
                                RestPermissions.STREAMS_READ,
                                RestPermissions.STREAMS_EDIT,
                                RestPermissions.STREAMS_CHANGESTATE,
                                RestPermissions.DASHBOARDS_READ,
                                RestPermissions.DASHBOARDS_EDIT,
                                RestPermissions.EVENT_DEFINITIONS_READ,
                                RestPermissions.EVENT_DEFINITIONS_EDIT
                                // TODO: Add missing collection permissions
                        )
                ))
                .put(Capability.OWN, CapabilityDescriptor.create(
                        Capability.OWN,
                        "Owner",
                        ImmutableSet.of(
                                RestPermissions.ENTITY_OWN,
                                RestPermissions.STREAMS_READ,
                                RestPermissions.STREAMS_EDIT,
                                RestPermissions.STREAMS_CHANGESTATE,
                                RestPermissions.DASHBOARDS_READ,
                                RestPermissions.DASHBOARDS_EDIT,
                                RestPermissions.EVENT_DEFINITIONS_READ,
                                RestPermissions.EVENT_DEFINITIONS_EDIT
                                // TODO: Add missing collection permissions
                        )
                ))
                /* TODO: Needs to be moved to the actual roles registry
                .put(ROLE_COLLECTION_CREATOR, CapabilityDTO.create(
                        grnRegistry.parse(ROLE_COLLECTION_CREATOR).entity(),
                        "Collection Creator",
                        // TODO this is an enterprise role, do we want pluggable roles?
                        // TODO or another solution?
                        // ImmutableSet.of(AdditionalRestPermissions.COLLECTION_CREATE)
                        ImmutableSet.of("collections:create")
                ))
                .put(ROLE_DASHBOARD_CREATOR, CapabilityDTO.create(
                        grnRegistry.parse(ROLE_DASHBOARD_CREATOR).entity(),
                        "Dashboard Creator",
                        ImmutableSet.of(RestPermissions.DASHBOARDS_CREATE)
                ))
                .put(ROLE_STREAM_CREATOR, CapabilityDTO.create(
                        grnRegistry.parse(ROLE_STREAM_CREATOR).entity(),
                        "Stream Creator",
                        ImmutableSet.of(RestPermissions.STREAMS_CREATE)
                ))
                 */
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
