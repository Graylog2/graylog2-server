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
        CAPABILITIES = ImmutableMap.<Capability, CapabilityDescriptor>builder()
                .put(Capability.VIEW, CapabilityDescriptor.create(
                        Capability.VIEW,
                        "Viewer",
                        ImmutableSet.of(
                                RestPermissions.STREAMS_READ,
                                RestPermissions.DASHBOARDS_READ,
                                ViewsRestPermissions.VIEW_READ,
                                RestPermissions.EVENT_DEFINITIONS_READ,
                                RestPermissions.EVENT_NOTIFICATIONS_READ
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
                                ViewsRestPermissions.VIEW_READ,
                                ViewsRestPermissions.VIEW_EDIT,
                                RestPermissions.EVENT_DEFINITIONS_READ,
                                RestPermissions.EVENT_DEFINITIONS_EDIT,
                                RestPermissions.EVENT_NOTIFICATIONS_READ,
                                RestPermissions.EVENT_NOTIFICATIONS_EDIT
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
                                ViewsRestPermissions.VIEW_READ,
                                ViewsRestPermissions.VIEW_EDIT,
                                ViewsRestPermissions.VIEW_DELETE,
                                RestPermissions.EVENT_DEFINITIONS_READ,
                                RestPermissions.EVENT_DEFINITIONS_EDIT,
                                RestPermissions.EVENT_DEFINITIONS_DELETE,
                                RestPermissions.EVENT_NOTIFICATIONS_READ,
                                RestPermissions.EVENT_NOTIFICATIONS_EDIT,
                                RestPermissions.EVENT_NOTIFICATIONS_DELETE
                        )
                ))
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
