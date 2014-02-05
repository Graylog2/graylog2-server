/*
 * Copyright 2013 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security.realm;

import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog2.Core;
import org.graylog2.database.ValidationException;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenAuthToken;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessTokenAuthenticator extends AuthenticatingRealm {
    private static final Logger log = LoggerFactory.getLogger(AccessTokenAuthenticator.class);

    private final Core core;

    public AccessTokenAuthenticator(Core core) {
        this.core = core;
        setAuthenticationTokenClass(AccessTokenAuthToken.class);
        // the presence of a valid access token is enough, we don't have any other credentials
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        AccessTokenAuthToken authToken = (AccessTokenAuthToken) token;
        final AccessToken accessToken = AccessToken.load(String.valueOf(authToken.getToken()), core);

        if (accessToken == null) {
            return null;
        }
        final User user = User.load(accessToken.getUserName(), core);
        if (user == null) {
            return null;
        }
        if (user.isExternalUser() && !core.getLdapAuthenticator().isEnabled()) {
            throw new LockedAccountException("LDAP authentication is currently disabled.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Found user {} for access token.", user);
        }
        try {
            accessToken.touch();
        } catch (ValidationException e) {
            log.warn("Unable to update access token's last access date.", e);
        }
        return new SimpleAccount(user.getName(), null, "access token realm");
    }
}
