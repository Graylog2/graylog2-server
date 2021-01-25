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

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.shared.security.AccessTokenAuthToken;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class AccessTokenAuthenticator extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenAuthenticator.class);
    public static final String NAME = "access-token";

    private final AccessTokenService accessTokenService;
    private final UserService userService;

    @Inject
    AccessTokenAuthenticator(AccessTokenService accessTokenService,
                             UserService userService) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        setAuthenticationTokenClass(AccessTokenAuthToken.class);
        setCachingEnabled(false);
        // the presence of a valid access token is enough, we don't have any other credentials
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        AccessTokenAuthToken authToken = (AccessTokenAuthToken) token;
        final AccessToken accessToken = accessTokenService.load(String.valueOf(authToken.getToken()));

        if (accessToken == null) {
            return null;
        }
        // TODO should be using IDs
        final User user = userService.load(accessToken.getUserName());
        if (user == null) {
            return null;
        }
        if (!user.getAccountStatus().equals(User.AccountStatus.ENABLED)) {
            LOG.warn("Account for user <{}> is disabled.", user.getName());
            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Found user {} for access token.", user);
        }
        try {
            accessTokenService.touch(accessToken);
        } catch (ValidationException e) {
            LOG.warn("Unable to update access token's last access date.", e);
        }
        ShiroSecurityContext.requestSessionCreation(false);
        return new SimpleAccount(user.getId(), null, "access token realm");
    }
}
