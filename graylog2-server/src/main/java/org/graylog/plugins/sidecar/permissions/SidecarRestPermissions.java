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
package org.graylog.plugins.sidecar.permissions;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Collections;
import java.util.Set;

import static org.graylog2.plugin.security.Permission.create;

public class SidecarRestPermissions implements PluginPermissions {
    public static final String SIDECARS_READ = "sidecars:read";
    public static final String SIDECARS_CREATE = "sidecars:create";
    public static final String SIDECARS_UPDATE = "sidecars:update";
    public static final String SIDECARS_DELETE = "sidecar:delete";

    public static final String COLLECTORS_READ = "sidecars:read";
    public static final String COLLECTORS_CREATE = "sidecars:create";
    public static final String COLLECTORS_UPDATE = "sidecars:update";
    public static final String COLLECTORS_DELETE = "sidecars:delete";

    public static final String CONFIGURATIONS_READ = "configurations:read";
    public static final String CONFIGURATIONS_CREATE = "configurations:create";
    public static final String CONFIGURATIONS_UPDATE = "configurations:update";
    public static final String CONFIGURATIONS_DELETE = "configurations:delete";

    private final ImmutableSet<Permission> permissions = ImmutableSet.of(
            create(SIDECARS_READ, "Read sidecars"),
            create(SIDECARS_CREATE, "Create sidecars"),
            create(SIDECARS_UPDATE, "Update sidecars"),
            create(SIDECARS_DELETE, "Delete sidecars"),

            create(COLLECTORS_READ, "Read sidecars"),
            create(COLLECTORS_CREATE, "Create sidecars"),
            create(COLLECTORS_UPDATE, "Update sidecars"),
            create(COLLECTORS_DELETE, "Delete sidecars"),

            create(CONFIGURATIONS_READ, "Read configurations"),
            create(CONFIGURATIONS_CREATE, "Create configurations"),
            create(CONFIGURATIONS_UPDATE, "Update configurations"),
            create(CONFIGURATIONS_DELETE, "Delete configurations")
    );

    @Override
    public Set<Permission> permissions() {
        return permissions;
    }

    @Override
    public Set<Permission> readerBasePermissions() {
        return Collections.emptySet();
    }
}
