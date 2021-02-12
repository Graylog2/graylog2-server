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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.security.Permissions;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UserImplTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private PasswordAlgorithmFactory passwordAlgorithmFactory;

    private UserImpl user;

    @Test
    public void testFirstLastFullNames() {
        Map<String, Object> fields = ImmutableMap.of(
                UserImpl.FIRST_NAME, "First",
                UserImpl.LAST_NAME, "Last",
                UserImpl.USERNAME, "Another");
        user = new UserImpl(null, null, fields);
        assertEquals("First Last", user.getFullName());
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testFirstLastOnly() {
        Map<String, Object> fields = ImmutableMap.of(
                UserImpl.FIRST_NAME, "First",
                UserImpl.LAST_NAME, "Last");
        user = new UserImpl(null, null, fields);
        assertEquals("First Last", user.getFullName());
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testFullOnly() {
        Map<String, Object> fields = ImmutableMap.of(
                UserImpl.FULL_NAME, "Full Name");
        user = new UserImpl(null, null, fields);
        assertEquals("Full Name", user.getFullName());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
    }

    @Test
    public void getPermissionsWorksWithEmptyPermissions() throws Exception {
        final Permissions permissions = new Permissions(Collections.emptySet());
        final Map<String, Object> fields = Collections.singletonMap(UserImpl.USERNAME, "foobar");
        user = new UserImpl(passwordAlgorithmFactory, permissions, fields);
        assertThat(user.getPermissions()).containsAll(permissions.userSelfEditPermissions("foobar"));
    }

    @Test
    public void getPermissionsReturnsListOfPermissions() throws Exception {
        final Permissions permissions = new Permissions(Collections.emptySet());
        final List<String> customPermissions = Collections.singletonList("subject:action");
        final Map<String, Object> fields = ImmutableMap.of(
            UserImpl.USERNAME, "foobar",
            UserImpl.PERMISSIONS, customPermissions);
        user = new UserImpl(passwordAlgorithmFactory, permissions, fields);
        assertThat(user.getPermissions())
            .containsAll(permissions.userSelfEditPermissions("foobar"))
            .contains("subject:action");
    }

    @Test
    public void permissionsArentModified() {
        final Permissions permissions = new Permissions(Collections.emptySet());
        final Map<String, Object> fields = Collections.singletonMap(UserImpl.USERNAME, "foobar");
        user = new UserImpl(passwordAlgorithmFactory, permissions, fields);

        final List<String> newPermissions = ImmutableList.<String>builder()
                .addAll(user.getPermissions())
                .add("perm:1")
                .build();
        user.setPermissions(newPermissions);
    }

    @Test
    public void getObjectPermissions() {
        final Permissions permissions = new Permissions(Collections.emptySet());
        final List<String> customPermissions = ImmutableList.of("subject:action", "*");
        final Map<String, Object> fields = ImmutableMap.of(
                UserImpl.USERNAME, "foobar",
                UserImpl.PERMISSIONS, customPermissions);
        user = new UserImpl(passwordAlgorithmFactory, permissions, fields);

        final Set<Permission> userSelfEditPermissions = permissions.userSelfEditPermissions("foobar").stream().map(WildcardPermission::new).collect(Collectors.toSet());
        assertThat(user.getObjectPermissions())
                .containsAll(userSelfEditPermissions)
                .contains(new WildcardPermission("subject:action"))
                .extracting("class").containsOnlyOnce(AllPermission.class);
    }
}
