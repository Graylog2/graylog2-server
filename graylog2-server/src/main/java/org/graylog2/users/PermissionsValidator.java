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
package org.graylog2.users;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.graylog.security.UserContext;
import org.graylog2.rest.models.users.requests.CreateUserRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.shared.security.RestPermissions.ROLES_READ;

public class PermissionsValidator {
    private final RoleService roleService;

    @Inject
    public PermissionsValidator(RoleService roleService) {
        this.roleService = roleService;
    }

    public void validatePermissionsAndRoles(@NotNull CreateUserRequest cr, UserContext userContext) {
        validatePermissionsAndRoles(cr.roles(), cr.permissions(), userContext);
    }

    public void validateRolePermissions(List<String> requestRoles, UserContext userContext) {
        validatePermissionsAndRoles(requestRoles, List.of(), userContext);
    }

    private void validatePermissionsAndRoles(List<String> requestRoles, List<String> requestPermissions, UserContext userContext) {
        try {
            final var rolePermissions = extractPermissionsFromRoles(requestRoles, userContext::isPermitted);
            final var joinedPermissions = Stream.concat(rolePermissions.stream(), requestPermissions.stream()).collect(Collectors.toSet());
            validatePermissions(joinedPermissions, userContext);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new BadRequestException(e);
        }
    }

    public void validatePermissions(Collection<String> permissions, UserContext userContext) {
        final var missingPermissions = permissionsCurrentUserMisses(permissions, userContext);
        if (!missingPermissions.isEmpty()) {
            throw new BadRequestException("Cannot assign permissions/roles to new user that current user does not have: " + String.join(", ", missingPermissions));
        }
    }

    private Set<String> extractPermissionsFromRoles(List<String> requestRoles, BiPredicate<String, String> isPermitted) throws org.graylog2.database.NotFoundException {
        final var normalizedRoles = Optional.ofNullable(requestRoles).orElse(Collections.emptyList());
        final var deniedRoles = normalizedRoles
                .stream()
                .filter(role -> !isPermitted.test(ROLES_READ, role))
                .toList();
        if (!deniedRoles.isEmpty()) {
            throw new ForbiddenException("Not allowed to read roles: " + String.join(", ", deniedRoles));
        }
        final var roles = roleService.loadByNames(normalizedRoles);
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toSet());
    }

    private Set<String> permissionsCurrentUserMisses(Collection<String> requestPermissions, UserContext user) {
        final var normalizedPermissions = Optional.ofNullable(requestPermissions).orElse(Collections.emptyList());
        return normalizedPermissions.stream()
                .filter(permission -> !user.isPermitted(permission))
                .collect(Collectors.toSet());
    }
}
