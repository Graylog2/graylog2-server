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
package org.graylog.security;

import com.google.common.collect.ImmutableSet;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.SecurityTestUtils;
import org.graylog2.shared.rest.RequestIdFilter;
import org.graylog2.shared.security.ShiroRequestHeadersBinder;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserContextTest {


    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
    }

    @Test
    void UserContextWithoutContext() {
        assertThatExceptionOfType(UserContextMissingException.class).isThrownBy(() -> new UserContext.Factory(userService).create());
    }

    @Test
    void runAs() {

        final String userName = "user";
        final String roleName = "role";
        final Set<String> permissions = ImmutableSet.of("permission1", "permission2");
        String password = "test_password";

        SecurityTestUtils.TestRealm realm = new SecurityTestUtils.TestRealm();
        realm.addRole(roleName, permissions);
        realm.addUser(userName, password, roleName);

        SecurityManager securityManager = new DefaultSecurityManager(realm);
        SecurityUtils.setSecurityManager(securityManager);

        final User user = mock(User.class);
        when(user.getId()).thenReturn(userName);
        when(userService.load(userName)).thenReturn(user);
        when(userService.loadById(userName)).thenReturn(user);

        UserContext.<Void>runAs(userName, userService, () -> {

            final UserContext userContext = new UserContext.Factory(userService).create();
            // test user context basics
            assertThat(userContext.getUserId()).isEqualTo(userName);
            assertThat(userContext.getUser()).isEqualTo(user);

            // test permission check
            assertThat(userContext.isPermitted("permission1")).isTrue();
            assertThat(userContext.isPermitted("permission2")).isTrue();
            assertThat(userContext.isPermitted("permission3")).isFalse();

            // test request header fix
            Object requestHeaders = ThreadContext.get(ShiroRequestHeadersBinder.REQUEST_HEADERS);
            assertThat(requestHeaders).isNotNull();
            assertThat(requestHeaders).isInstanceOf(MultivaluedMap.class);
            assertThat((MultivaluedMap<String, String>) requestHeaders).containsKey(RequestIdFilter.X_REQUEST_ID);

            return null;
        });

        assertThat(ThreadContext.get(ShiroRequestHeadersBinder.REQUEST_HEADERS)).isNull();

    }
}
