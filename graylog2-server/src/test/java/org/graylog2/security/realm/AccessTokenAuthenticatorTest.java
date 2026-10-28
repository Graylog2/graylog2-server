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
package org.graylog2.security.realm;

import org.apache.shiro.authc.AuthenticationInfo;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.shared.security.AccessTokenAuthToken;
import org.graylog2.shared.security.AccessTokenPrincipal;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessTokenAuthenticatorTest {
    private AccessTokenService accessTokenService;
    private AccessTokenAuthenticator authenticator;
    private AccessToken accessToken;

    @BeforeEach
    void setUp() {
        accessTokenService = mock(AccessTokenService.class);
        final UserService userService = mock(UserService.class);
        authenticator = new AccessTokenAuthenticator(accessTokenService, userService);

        final User user = mock(User.class);
        when(user.getId()).thenReturn("user-id");
        when(user.getAccountStatus()).thenReturn(User.AccountStatus.ENABLED);
        when(userService.load("jane")).thenReturn(user);

        accessToken = mock(AccessToken.class);
        when(accessToken.getId()).thenReturn("token-id");
        when(accessToken.getUserName()).thenReturn("jane");
        when(accessTokenService.load("secret")).thenReturn(accessToken);
    }

    @Test
    void keepsUserIdAsPrimaryPrincipalAndAddsTokenPrincipal() {
        when(accessToken.getAudience()).thenReturn("mcp");
        when(accessToken.getExpiresAt()).thenReturn(Tools.nowUTC().plusHours(1));

        final AuthenticationInfo info = authenticator.doGetAuthenticationInfo(new AccessTokenAuthToken("secret", "127.0.0.1"));

        assertThat(info.getPrincipals().getPrimaryPrincipal()).isEqualTo("user-id");
        assertThat(info.getPrincipals().oneByType(AccessTokenPrincipal.class)).isEqualTo(new AccessTokenPrincipal("token-id", "mcp"));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        when(accessToken.getExpiresAt()).thenReturn(Tools.nowUTC().minusMinutes(1));

        assertThat(authenticator.doGetAuthenticationInfo(new AccessTokenAuthToken("secret", "127.0.0.1"))).isNull();
        verify(accessTokenService, never()).touch(accessToken);
    }
}
