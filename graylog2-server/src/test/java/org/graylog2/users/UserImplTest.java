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
import org.graylog.security.permissions.CaseSensitiveWildcardPermission;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.PeriodDuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImplTest {

    @Mock
    private PasswordAlgorithmFactory passwordAlgorithmFactory;

    @Mock
    private ClusterConfigService clusterConfigService;

    private UserImpl user;

    private UserImpl createUserImpl(PasswordAlgorithmFactory passwordAlgorithmFactory,
                                    Permissions permissions,
                                    Map<String, Object> fields) {
        return new UserImpl(passwordAlgorithmFactory, permissions, clusterConfigService, fields);
    }

    @Test
    void testFirstLastFullNames() {
        user = createUserImpl(null, null, null);
        user.setFirstLastFullNames("First", "Last");
        assertTrue(user.getFirstName().isPresent());
        assertTrue(user.getLastName().isPresent());
        assertEquals("First", user.getFirstName().get());
        assertEquals("Last", user.getLastName().get());
        assertEquals("First Last", user.getFullName());
    }

    @Test
    void testSetFullName() {
        user = createUserImpl(null, null, null);
        user.setFullName("Full Name");
        assertEquals("Full Name", user.getFullName());
        assertFalse(user.getFirstName().isPresent());
        assertFalse(user.getLastName().isPresent());
    }

    @Test
    void testNoFullNameEmptyString() {
        user = createUserImpl(null, null, null);
        assertEquals("", user.getFullName());
    }

    @Test
    void testFirstLastRequired() {
        user = createUserImpl(null, null, null);
        assertThatThrownBy(() -> user.setFirstLastFullNames(null, "Last"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A firstName value is required");

        assertThatThrownBy(() -> user.setFirstLastFullNames("First", null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A lastName value is required");
    }

    @Test
    void testFirstNameLengthValidation() {
        user = createUserImpl(null, null, null);
        ValidationResult result = user.getValidations().get(UserImpl.FIRST_NAME)
                                            .validate(StringUtils.repeat("*", 10));
        assertTrue(result.passed());
        result = user.getValidations().get(UserImpl.FIRST_NAME)
                     .validate(StringUtils.repeat("*", 210));
        assertFalse(result.passed());
    }

    @Test
    void testLastNameLengthValidation() {
        user = createUserImpl(null, null, null);
        ValidationResult result = user.getValidations().get(UserImpl.LAST_NAME)
                                      .validate(StringUtils.repeat("*", 10));
        assertTrue(result.passed());
        result = user.getValidations().get(UserImpl.LAST_NAME)
                     .validate(StringUtils.repeat("*", 210));
        assertFalse(result.passed());
    }

    @Test
    void getPermissionsWorksWithEmptyPermissions() {
        final Permissions permissions = new Permissions(Collections.emptySet());
        final Map<String, Object> fields = Collections.singletonMap(UserImpl.USERNAME, "foobar");
        user = createUserImpl(passwordAlgorithmFactory, permissions, fields);
        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(CONFIG_ALL_ALLOWED);

        assertThat(user.getPermissions()).containsAll(permissions.userSelfEditPermissions("foobar"));
    }

    public static Stream<Arguments> tokenPermissionParameters() {
        return Stream.of(
                Arguments.of(CONFIG_ALL_ALLOWED, false, true),
                Arguments.of(CONFIG_ALL_ALLOWED, true, true),
                Arguments.of(CONFIG_ALL_FORBIDDEN, false, false),
                Arguments.of(CONFIG_ALL_FORBIDDEN, true, false),
                Arguments.of(CONFIG_EXTERNAL_FORBIDDEN, true, false),
                Arguments.of(CONFIG_EXTERNAL_FORBIDDEN, false, true)
        );
    }

    @ParameterizedTest
    @MethodSource("tokenPermissionParameters")
    void testTokenCreatePermissionsParameterised(UserConfiguration config, boolean isExternal, boolean isAllowedToCreateTokens) {
        final String username = "foobar";
        final Permissions permissions = new Permissions(Collections.emptySet());
        final Map<String, Object> fields = Map.of(UserImpl.USERNAME, username, UserImpl.EXTERNAL_USER, isExternal);
        user = createUserImpl(passwordAlgorithmFactory, permissions, fields);
        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(config);

        if (isAllowedToCreateTokens) {
            assertThat(user.getPermissions()).contains(RestPermissions.USERS_TOKENCREATE + ":" + username);
        } else {
            assertThat(user.getPermissions()).doesNotContain(RestPermissions.USERS_TOKENCREATE + ":" + username);
        }
    }

    @Test
    void getPermissionsReturnsListOfPermissions() {
        final Permissions permissions = new Permissions(Collections.emptySet());
        final List<String> customPermissions = Collections.singletonList("subject:action");
        final Map<String, Object> fields = ImmutableMap.of(
            UserImpl.USERNAME, "foobar",
            UserImpl.PERMISSIONS, customPermissions);
        user = createUserImpl(passwordAlgorithmFactory, permissions, fields);
        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(CONFIG_ALL_ALLOWED);

        assertThat(user.getPermissions())
            .containsAll(permissions.userSelfEditPermissions("foobar"))
            .contains("subject:action");
    }

    @Test
    void permissionsArentModified() {
        final Permissions permissions = new Permissions(Collections.emptySet());
        final Map<String, Object> fields = Collections.singletonMap(UserImpl.USERNAME, "foobar");
        user = createUserImpl(passwordAlgorithmFactory, permissions, fields);
        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(CONFIG_ALL_ALLOWED);

        final List<String> newPermissions = ImmutableList.<String>builder()
                .addAll(user.getPermissions())
                .add("perm:1")
                .build();
        user.setPermissions(newPermissions);

        assertThat(user.getPermissions()).hasSameElementsAs(newPermissions);
    }

    @Test
    void getObjectPermissions() {
        final Permissions permissions = new Permissions(Collections.emptySet());
        final List<String> customPermissions = List.of("subject:action", "*");
        final Map<String, Object> fields = ImmutableMap.of(
                UserImpl.USERNAME, "foobar",
                UserImpl.PERMISSIONS, customPermissions);
        user = createUserImpl(passwordAlgorithmFactory, permissions, fields);
        when(clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(CONFIG_ALL_ALLOWED);

        final Set<Permission> userSelfEditPermissions = permissions.userSelfEditPermissions("foobar").stream().map(CaseSensitiveWildcardPermission::new).collect(Collectors.toSet());
        assertThat(user.getObjectPermissions())
                .containsAll(userSelfEditPermissions)
                .contains(new CaseSensitiveWildcardPermission("subject:action"))
                .extracting("class").containsOnlyOnce(AllPermission.class);
    }

    private static final UserConfiguration CONFIG_ALL_ALLOWED = UserConfiguration.create(false, Duration.of(8, ChronoUnit.HOURS), true, false, PeriodDuration.parse("P30D"));
    private static final UserConfiguration CONFIG_ALL_FORBIDDEN = UserConfiguration.create(false, Duration.of(8, ChronoUnit.HOURS), false, true, PeriodDuration.parse("P30D"));
    private static final UserConfiguration CONFIG_EXTERNAL_FORBIDDEN = UserConfiguration.create(false, Duration.of(8, ChronoUnit.HOURS), false, false, PeriodDuration.parse("P30D"));
}
