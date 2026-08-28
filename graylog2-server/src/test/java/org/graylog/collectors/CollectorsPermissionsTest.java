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
package org.graylog.collectors;

import org.graylog2.plugin.security.Permission;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CollectorsPermissionsTest {

    private final CollectorsPermissions permissions = new CollectorsPermissions();

    private Set<String> permissionStrings() {
        return permissions.permissions().stream()
                .map(Permission::permission)
                .collect(Collectors.toSet());
    }

    @Test
    void declaresEnrollmentTokenPermissions() {
        assertThat(CollectorsPermissions.ENROLL_TOKEN_CREATE).isEqualTo("collector_enrollment_tokens:create");
        assertThat(CollectorsPermissions.ENROLL_TOKEN_READ).isEqualTo("collector_enrollment_tokens:read");
        assertThat(CollectorsPermissions.ENROLL_TOKEN_DELETE).isEqualTo("collector_enrollment_tokens:delete");

        assertThat(permissionStrings()).contains(
                CollectorsPermissions.ENROLL_TOKEN_CREATE,
                CollectorsPermissions.ENROLL_TOKEN_READ,
                CollectorsPermissions.ENROLL_TOKEN_DELETE);
    }

    @Test
    void shipsCollectorsManagerRole() {
        final var role = permissions.builtinRoles().stream()
                .filter(r -> "Collectors Manager".equals(r.name()))
                .findFirst()
                .orElseThrow();

        assertThat(role.permissions()).containsExactlyInAnyOrder(
                CollectorsPermissions.FLEET_CREATE,
                CollectorsPermissions.FLEET_READ,
                CollectorsPermissions.FLEET_EDIT,
                CollectorsPermissions.FLEET_DELETE,
                CollectorsPermissions.FLEET_INSTANCE_ASSIGN,
                CollectorsPermissions.FLEET_INSTANCE_DELETE,
                CollectorsPermissions.SOURCE_CREATE,
                CollectorsPermissions.SOURCE_EDIT,
                CollectorsPermissions.SOURCE_DELETE,
                CollectorsPermissions.ENROLL_TOKEN_CREATE,
                CollectorsPermissions.ENROLL_TOKEN_READ,
                CollectorsPermissions.ENROLL_TOKEN_DELETE,
                CollectorsPermissions.CONFIGURATION_READ,
                CollectorsPermissions.CONFIGURATION_EDIT,
                CollectorsPermissions.ACTIVITIES_READ);
    }

    @Test
    void shipsCollectorsReaderRole() {
        final var role = permissions.builtinRoles().stream()
                .filter(r -> "Collectors Reader".equals(r.name()))
                .findFirst()
                .orElseThrow();

        assertThat(role.permissions()).containsExactlyInAnyOrder(
                CollectorsPermissions.FLEET_READ,
                CollectorsPermissions.CONFIGURATION_READ,
                CollectorsPermissions.ACTIVITIES_READ);
    }

    @Test
    void everyBuiltinRolePermissionIsDeclared() {
        final Set<String> declared = permissionStrings();

        permissions.builtinRoles().forEach(role ->
                assertThat(declared).as("role <%s>", role.name()).containsAll(role.permissions()));
    }
}
