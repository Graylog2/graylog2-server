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
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog2.plugin.database.validators.ValidationResult;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserImplTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private PasswordAlgorithmFactory passwordAlgorithmFactory;

    private UserImpl user;

    @Test
    public void testFirstLastFullNames() {
        user = new UserImpl(null, null, null);
        user.setFullName("First", "Last");
        assertEquals("First Last", user.getFullName());
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testSetFullNameDeprecated() {
        user = new UserImpl(null, null, null);
        assertThatThrownBy(() -> user.setFullName("Full Name"))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Use setFullName");
    }

    @Test
    public void testFirstLastRequired() {
        user = new UserImpl(null, null, null);
        assertThatThrownBy(() -> user.setFullName(null, "Last"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A firstName value is required");

        assertThatThrownBy(() -> user.setFullName("First", null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A lastName value is required");
    }

    @Test
    public void testFirstNameLengthValidation() {
        user = new UserImpl(null, null, null);
        ValidationResult result = user.getValidations().get(UserImpl.FIRST_NAME)
                                            .validate(StringUtils.repeat("*", 10));
        assertTrue(result.passed());
        result = user.getValidations().get(UserImpl.FIRST_NAME)
                     .validate(StringUtils.repeat("*", 210));
        assertFalse(result.passed());
    }

    @Test
    public void testLastNameLengthValidation() {
        user = new UserImpl(null, null, null);
        ValidationResult result = user.getValidations().get(UserImpl.LAST_NAME)
                                      .validate(StringUtils.repeat("*", 10));
        assertTrue(result.passed());
        result = user.getValidations().get(UserImpl.LAST_NAME)
                     .validate(StringUtils.repeat("*", 210));
        assertFalse(result.passed());
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
