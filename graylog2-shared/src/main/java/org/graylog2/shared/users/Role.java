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
package org.graylog2.shared.users;

import com.google.common.base.Function;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public interface Role {
    String getId();

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    Set<String> getPermissions();

    void setPermissions(Set<String> permissions);

    boolean isReadOnly();

    class RoleIdToNameFunction implements Function<String, String> {
        private final Map<String, Role> idToRole;

        public RoleIdToNameFunction(Map<String, Role> idToRole) {
            this.idToRole = idToRole;
        }

        @Nullable
        @Override
        public String apply(String roleId) {
            if (roleId == null || !idToRole.containsKey(roleId)) {
                return null;
            }
            return idToRole.get(roleId).getName().toLowerCase();
        }
    }

    class RoleNameToIdFunction implements Function<String, String> {

        private final Map<String, Role> nameToRole;

        public RoleNameToIdFunction(Map<String, Role> nameToRole) {
            this.nameToRole = nameToRole;
        }

        @Nullable
        @Override
        public String apply(@Nullable String roleName) {
            if (roleName == null) {
                return null;
            }
            final Role role = nameToRole.get(roleName.toLowerCase());
            if (role == null) {
                return null;
            }
            return role.getId();
        }
    }

    class RoleToNameFunction implements Function<Role, String> {
        @Nullable
        @Override
        public String apply(@Nullable Role input) {
            return input != null ? input.getName().toLowerCase() : null;
        }
    }
}
