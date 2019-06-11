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
