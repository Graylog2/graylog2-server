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

import org.apache.shiro.authz.Permission;
import org.graylog.grn.GRN;

import java.util.Set;

/**
 * Resolves a principal to specific permissions based on grants.
 */
public interface PermissionAndRoleResolver {
    /**
     * Returns resolved permissions for the given principal.
     *
     * @param principal the principal
     * @return the resolved permissions
     */
    Set<Permission> resolvePermissionsForPrincipal(GRN principal);

    /**
     * Returns roles for the given principal.
     *
     * @param principal the principal
     * @return the resolved roleIds
     */
    Set<String> resolveRolesForPrincipal(GRN principal);
}
