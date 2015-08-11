package org.graylog2.shared.users;

import com.google.common.base.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public abstract class Roles {

    @Nonnull
    public static RoleIdToNameFunction roleIdToNameFunction(Map<String, Role> idMap) {
        return new RoleIdToNameFunction(idMap);
    }

    @Nonnull
    public static RoleNameToIdFunction roleNameToIdFunction(Map<String, Role> nameMap) {
        return new RoleNameToIdFunction(nameMap);
    }

    public static RoleToNameFunction roleToNameFunction() {
        return new RoleToNameFunction();
    }

    private static class RoleIdToNameFunction implements Function<String, String> {
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

    private static class RoleNameToIdFunction implements Function<String, String> {

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

    private static class RoleToNameFunction implements Function<Role, String> {
        @Nullable
        @Override
        public String apply(@Nullable Role input) {
            return input != null ? input.getName().toLowerCase() : null;
        }
    }
}
