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

package org.graylog2.migrations;

import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class V20250721090000_AddClusterConfigurationPermissionTest {

    @Mock
    ClusterConfigService clusterConfigService;
    @Mock
    RoleService roleService;
    @Mock
    UserService userService;

    V20250721090000_AddClusterConfigurationPermission migration;

    @BeforeEach
    public void setUp() {
        when(clusterConfigService.get(V20250721090000_AddClusterConfigurationPermission.MigrationCompleted.class))
                .thenReturn(null);
        migration = new V20250721090000_AddClusterConfigurationPermission(clusterConfigService, roleService, userService);
    }

    @Test
    public void nothingMigratedIfReaderRoleNotFound() throws NotFoundException {
        String readerId = UUID.randomUUID().toString();
        when(roleService.getReaderRoleObjectId()).thenReturn(readerId);
        when(roleService.loadById(readerId)).thenThrow(NotFoundException.class);
        migration.upgrade();
        verifyNoInteractions(userService);
        verify(clusterConfigService, times(1)).write(any());
    }

    @Test
    public void nothingMigratedIfClusterConfigurationReaderRoleNotFound() throws NotFoundException {
        String readerId = UUID.randomUUID().toString();
        when(roleService.getReaderRoleObjectId()).thenReturn(readerId);
        when(roleService.loadById(readerId)).thenReturn(mock(Role.class));
        when(roleService.load(V20250721090000_AddClusterConfigurationPermission.CLUSTER_CONFIGURATION_READER_ROLE))
                .thenThrow(NotFoundException.class);
        migration.upgrade();
        verifyNoInteractions(userService);
        verify(clusterConfigService, times(1)).write(any());
    }

    @Test
    public void roleAddedToAllUsersWithReaderRole() throws NotFoundException, ValidationException {
        String readerId = UUID.randomUUID().toString();
        String clusterConfigurationReaderRoleId = UUID.randomUUID().toString();
        when(roleService.getReaderRoleObjectId()).thenReturn(readerId);
        Role readerRole = mock(Role.class);
        when(roleService.loadById(readerId)).thenReturn(readerRole);
        Role clusterConfigurationReaderRole = mock(Role.class);
        when(clusterConfigurationReaderRole.getId()).thenReturn(clusterConfigurationReaderRoleId);
        when(roleService.load(V20250721090000_AddClusterConfigurationPermission.CLUSTER_CONFIGURATION_READER_ROLE))
                .thenReturn(clusterConfigurationReaderRole);

        User user1 = mock(User.class);
        when(user1.getRoleIds()).thenReturn(new HashSet<>(List.of(readerId)));
        User user2 = mock(User.class);
        when(user2.getRoleIds()).thenReturn(new HashSet<>(List.of(readerId)));

        when(userService.loadAllForRole(readerRole)).thenReturn(List.of(user1, user2));

        migration.upgrade();

        verify(user1, times(1)).setRoleIds(Set.of(readerId, clusterConfigurationReaderRoleId));
        verify(user2, times(1)).setRoleIds(Set.of(readerId, clusterConfigurationReaderRoleId));
        verify(userService, times(2)).save(any());
        verify(clusterConfigService, times(1)).write(any());
    }

    @Test
    public void migrationRunsOnlyOnce() {
        when(clusterConfigService.get(V20250721090000_AddClusterConfigurationPermission.MigrationCompleted.class))
                .thenReturn(new V20250721090000_AddClusterConfigurationPermission.MigrationCompleted());
        migration.upgrade();
        verifyNoMoreInteractions(ignoreStubs(clusterConfigService));
        verifyNoInteractions(roleService, userService);
    }

}
