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
import org.graylog2.utilities.GRNRegistry;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class BuiltinRoles {
    private static ImmutableMap<String, RoleDTO> ROLES;

    public final static String ROLE_ENTITY_VIEWER = "grn::::role:54e3deadbeefdeadbeef0000";
    public final static String ROLE_ENTITY_MANAGER = "grn::::role:54e3deadbeefdeadbeef0001";
    public final static String ROLE_ENTITY_OWNER = "grn::::role:54e3deadbeefdeadbeef0002";

    public final static String ROLE_COLLECTION_CREATOR = "grn::::role:54e3deadbeefdeadbeef1001";
    public final static String ROLE_DASHBOARD_CREATOR = "grn::::role:54e3deadbeefdeadbeef1002";
    public final static String ROLE_STREAM_CREATOR = "grn::::role:54e3deadbeefdeadbeef1003";

    @Inject
    public BuiltinRoles(GRNRegistry grnRegistry) {

        ROLES = ImmutableMap.<String, RoleDTO>builder()
                .put(ROLE_ENTITY_VIEWER, RoleDTO.create(
                        grnRegistry.parse(ROLE_ENTITY_VIEWER).entity(),
                        "Viewer",
                        ImmutableSet.of(
                                RestPermissions.STREAMS_READ,
                                RestPermissions.DASHBOARDS_READ
                                // TODO: Add missing collection permissions
                        )
                ))
                .put(ROLE_ENTITY_MANAGER, RoleDTO.create(
                        grnRegistry.parse(ROLE_ENTITY_MANAGER).entity(),
                        "Manager",
                        ImmutableSet.of(
                                RestPermissions.STREAMS_READ,
                                RestPermissions.STREAMS_EDIT,
                                RestPermissions.STREAMS_CHANGESTATE,
                                RestPermissions.DASHBOARDS_READ,
                                RestPermissions.DASHBOARDS_EDIT
                                // TODO: Add missing collection permissions
                        )
                ))
                .put(ROLE_ENTITY_OWNER, RoleDTO.create(
                        grnRegistry.parse(ROLE_ENTITY_OWNER).entity(),
                        "Owner",
                        ImmutableSet.of(
                                RestPermissions.ENTITY_OWN,
                                RestPermissions.STREAMS_READ,
                                RestPermissions.STREAMS_EDIT,
                                RestPermissions.STREAMS_CHANGESTATE,
                                RestPermissions.DASHBOARDS_READ,
                                RestPermissions.DASHBOARDS_EDIT
                                // TODO: Add missing collection permissions
                        )
                ))
                .put(ROLE_COLLECTION_CREATOR, RoleDTO.create(
                        grnRegistry.parse(ROLE_COLLECTION_CREATOR).entity(),
                        "Collection Creator",
                        // TODO this is an enterprise role, do we want pluggable roles?
                        // TODO or another solution?
                        // ImmutableSet.of(AdditionalRestPermissions.COLLECTION_CREATE)
                        ImmutableSet.of("collections:create")
                ))
                .put(ROLE_DASHBOARD_CREATOR, RoleDTO.create(
                        grnRegistry.parse(ROLE_DASHBOARD_CREATOR).entity(),
                        "Dashboard Creator",
                        ImmutableSet.of(RestPermissions.DASHBOARDS_CREATE)
                ))
                .put(ROLE_STREAM_CREATOR, RoleDTO.create(
                        grnRegistry.parse(ROLE_STREAM_CREATOR).entity(),
                        "Stream Creator",
                        ImmutableSet.of(RestPermissions.STREAMS_CREATE)
                ))
                .build();
    }

    public static ImmutableSet<RoleDTO> allSharingRoles() {
        return ImmutableSet.of(
                ROLES.get(ROLE_ENTITY_VIEWER),
                ROLES.get(ROLE_ENTITY_MANAGER),
                ROLES.get(ROLE_ENTITY_OWNER)
        );
    }

    public Optional<RoleDTO> get(String grn) {
        return Optional.ofNullable(ROLES.get(grn));
    }
}
