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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mongodb.DuplicateKeyException;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.security.hashing.BCryptPasswordAlgorithm;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MigrationHelpersTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    public UserService userService;
    @Mock
    public RoleService roleService;

    private MigrationHelpers migrationHelpers;

    @Before
    public void setUp() throws Exception {
        this.migrationHelpers = new MigrationHelpers(roleService, userService);
    }

    @Test
    public void ensureBuiltinRole() throws Exception {
        final Role newRole = mock(Role.class);

        when(newRole.getId()).thenReturn("new-id");

        when(roleService.load("test-role")).thenThrow(NotFoundException.class);
        when(roleService.save(any(Role.class))).thenReturn(newRole);

        assertThat(migrationHelpers.ensureBuiltinRole("test-role", "description", ImmutableSet.of("a", "b")))
                .isEqualTo("new-id");

        final ArgumentCaptor<Role> roleArg = ArgumentCaptor.forClass(Role.class);

        verify(roleService, times(1)).save(roleArg.capture());

        assertThat(roleArg.getValue()).satisfies(role -> {
            assertThat(role.getName()).describedAs("role name").isEqualTo("test-role");
            assertThat(role.getDescription()).describedAs("role description").isEqualTo("description");
            assertThat(role.isReadOnly()).describedAs("role is read-only").isTrue();
            assertThat(role.getPermissions()).describedAs("role permissions").containsOnly("a", "b");
        });
    }

    @Test
    public void ensureBuiltinRoleWithoutReadOnly() throws Exception {
        final Role existingRole = mock(Role.class);

        when(existingRole.getId()).thenReturn("new-id");
        when(existingRole.isReadOnly()).thenReturn(false); // The existing role needs to be read-only

        when(roleService.load("test-role")).thenReturn(existingRole);
        when(roleService.save(any(Role.class))).thenReturn(existingRole);

        assertThat(migrationHelpers.ensureBuiltinRole("test-role", "description", ImmutableSet.of("a", "b")))
                .isEqualTo("new-id");

        final ArgumentCaptor<Role> roleArg = ArgumentCaptor.forClass(Role.class);

        verify(roleService, times(1)).save(roleArg.capture());

        assertThat(roleArg.getValue()).satisfies(role -> {
            assertThat(role.getName()).describedAs("role name").isEqualTo("test-role");
            assertThat(role.getDescription()).describedAs("role description").isEqualTo("description");
            assertThat(role.isReadOnly()).describedAs("role is read-only").isTrue();
            assertThat(role.getPermissions()).describedAs("role permissions").containsOnly("a", "b");
        });
    }

    @Test
    public void ensureBuiltinRoleWithInvalidPermissions() throws Exception {
        final Role existingRole = mock(Role.class);

        when(existingRole.getId()).thenReturn("new-id");
        when(existingRole.isReadOnly()).thenReturn(true);
        when(existingRole.getPermissions()).thenReturn(ImmutableSet.of("a")); // The existing role has wrong permissions

        when(roleService.load("test-role")).thenReturn(existingRole);
        when(roleService.save(any(Role.class))).thenReturn(existingRole);

        assertThat(migrationHelpers.ensureBuiltinRole("test-role", "description", ImmutableSet.of("a", "b")))
                .isEqualTo("new-id");

        final ArgumentCaptor<Role> roleArg = ArgumentCaptor.forClass(Role.class);

        verify(roleService, times(1)).save(roleArg.capture());

        assertThat(roleArg.getValue()).satisfies(role -> {
            assertThat(role.getName()).describedAs("role name").isEqualTo("test-role");
            assertThat(role.getDescription()).describedAs("role description").isEqualTo("description");
            assertThat(role.isReadOnly()).describedAs("role is read-only").isTrue();
            assertThat(role.getPermissions()).describedAs("role permissions").containsOnly("a", "b");
        });
    }

    @Test
    public void ensureBuiltinRoleExists() throws Exception {
        final Role existingRole = mock(Role.class);

        when(existingRole.getId()).thenReturn("new-id");
        when(existingRole.isReadOnly()).thenReturn(true);
        when(existingRole.getPermissions()).thenReturn(ImmutableSet.of("a", "b"));

        when(roleService.load("test-role")).thenReturn(existingRole);

        assertThat(migrationHelpers.ensureBuiltinRole("test-role", "description", ImmutableSet.of("a", "b")))
                .isEqualTo("new-id");

        verify(roleService, never()).save(any()); // The existing role is fine, so we don't expect it to be modified
    }

    @Test
    public void ensureBuiltinRoleWithSaveError() throws Exception {
        when(roleService.load("test-role")).thenThrow(NotFoundException.class);
        when(roleService.save(any(Role.class))).thenThrow(DuplicateKeyException.class); // Throw database error

        assertThat(migrationHelpers.ensureBuiltinRole("test-role", "description", ImmutableSet.of("a", "b")))
                .isNull();
    }

    @Test
    public void ensureUser() throws Exception {
        final Permissions permissions = new Permissions(ImmutableSet.of());

        when(userService.load("test-user")).thenReturn(null);
        when(userService.create()).thenReturn(newUser(permissions));
        when(userService.save(any(User.class))).thenReturn("new-id");

        assertThat(migrationHelpers.ensureUser("test-user", "pass", "Test", "User",
                                               "test@example.com", ImmutableSet.of("54e3deadbeefdeadbeef0001",
                                                                                   "54e3deadbeefdeadbeef0002")))
                .isEqualTo("new-id");

        final ArgumentCaptor<User> userArg = ArgumentCaptor.forClass(User.class);

        verify(userService, times(1)).save(userArg.capture());

        assertThat(userArg.getValue()).satisfies(user -> {
            assertThat(user.getName()).describedAs("user name").isEqualTo("test-user");
            assertThat(user.getFullName()).describedAs("user full-name").isEqualTo("Test User");
            assertThat(user.getHashedPassword()).describedAs("user hashed password").isNotBlank();
            assertThat(user.getEmail()).describedAs("user email").isEqualTo("test@example.com");
            assertThat(user.isReadOnly()).describedAs("user is read-only").isFalse();
            assertThat(user.getPermissions()).describedAs("user permissions")
                    .containsOnlyElementsOf(permissions.userSelfEditPermissions("test-user"));
            assertThat(user.getRoleIds()).describedAs("user roles").containsOnly(
                    "54e3deadbeefdeadbeef0001",
                    "54e3deadbeefdeadbeef0002"
            );
            assertThat(user.getTimeZone()).describedAs("user timezone").isEqualTo(DateTimeZone.UTC);
        });
    }

    @Test
    public void ensureUserWithoutExpectedRoles() throws Exception {
        final Permissions permissions = new Permissions(ImmutableSet.of());
        final User existingUser = newUser(permissions);

        existingUser.setName("test-user");
        existingUser.setFullName("Test User");
        existingUser.setPassword("password");
        existingUser.setEmail("test@example.com");
        existingUser.setTimeZone(DateTimeZone.UTC);
        existingUser.setRoleIds(ImmutableSet.of()); // Set invalid role IDs so the use gets updated

        when(userService.load("test-user")).thenReturn(existingUser);
        when(userService.save(any(User.class))).thenReturn("new-id");

        assertThat(migrationHelpers.ensureUser("test-user", "pass", "Test", "User",
                                               "test@example.com", ImmutableSet.of("54e3deadbeefdeadbeef0001",
                                                                                   "54e3deadbeefdeadbeef0002")))
                .isEqualTo("new-id");

        final ArgumentCaptor<User> userArg = ArgumentCaptor.forClass(User.class);

        verify(userService, times(1)).save(userArg.capture());

        assertThat(userArg.getValue()).satisfies(user -> {
            assertThat(user.getName()).describedAs("user name").isEqualTo("test-user");
            assertThat(user.getFullName()).describedAs("user full-name").isEqualTo("Test User");
            assertThat(user.getHashedPassword()).describedAs("user hashed password").isNotBlank();
            assertThat(user.getEmail()).describedAs("user email").isEqualTo("test@example.com");
            assertThat(user.isReadOnly()).describedAs("user is read-only").isFalse();
            assertThat(user.getPermissions()).describedAs("user permissions")
                    .containsOnlyElementsOf(permissions.userSelfEditPermissions("test-user"));
            assertThat(user.getRoleIds()).describedAs("user roles").containsOnly(
                    "54e3deadbeefdeadbeef0001",
                    "54e3deadbeefdeadbeef0002"
            );
            assertThat(user.getTimeZone()).describedAs("user timezone").isEqualTo(DateTimeZone.UTC);
        });
    }

    @Test
    public void ensureUserWithSaveError() throws Exception {
        final Permissions permissions = new Permissions(ImmutableSet.of());

        when(userService.load("test-user")).thenReturn(null);
        when(userService.create()).thenReturn(newUser(permissions));
        when(userService.save(any(User.class))).thenThrow(ValidationException.class);

        assertThat(migrationHelpers.ensureUser("test-user", "pass", "Test", "User",
                                               "test@example.com", ImmutableSet.of("54e3deadbeefdeadbeef0001",
                                                                                   "54e3deadbeefdeadbeef0002")))
                .isNull();
    }

    private User newUser(Permissions permissions) {
        final BCryptPasswordAlgorithm passwordAlgorithm = new BCryptPasswordAlgorithm(10);
        final PasswordAlgorithmFactory passwordAlgorithmFactory = new PasswordAlgorithmFactory(Collections.emptyMap(), passwordAlgorithm);

        return new UserImpl(passwordAlgorithmFactory, permissions, ImmutableMap.of());
    }
}
