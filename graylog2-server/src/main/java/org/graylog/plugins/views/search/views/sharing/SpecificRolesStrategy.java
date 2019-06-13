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
package org.graylog.plugins.views.search.views.sharing;

import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.Role;
import org.graylog2.users.RoleService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SpecificRolesStrategy implements SharingStrategy<SpecificRoles> {
    private final RoleService roleService;

    @Inject
    public SpecificRolesStrategy(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public boolean isAllowedToSee(@Nullable User user, @NotNull SpecificRoles viewSharing) {
        if (user == null) {
            return false;
        }
        if (user.isLocalAdmin()) {
            return true;
        }

        final Map<String, Role> roles;
        try {
            roles = roleService.loadAllIdMap();
        } catch (NotFoundException e) {
            return false;
        }
        final Set<String> userRoleNames = user.getRoleIds()
                .stream()
                .map(roles::get)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .collect(Collectors.toSet());
        return user.isLocalAdmin() || viewSharing.roles().stream().anyMatch(userRoleNames::contains);
    }
}
