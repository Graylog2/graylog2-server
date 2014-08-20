/**
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
import org.graylog2.database.ValidationException;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenAuthToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.users.User;
import org.graylog2.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class AccessTokenAuthenticator extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenAuthenticator.class);

    private final AccessTokenService accessTokenService;
    private final UserService userService;
    private final LdapUserAuthenticator ldapAuthenticator;

    @Inject
    public AccessTokenAuthenticator(AccessTokenService accessTokenService,
                                    UserService userService,
                                    LdapUserAuthenticator ldapAuthenticator) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.ldapAuthenticator = ldapAuthenticator;
        setAuthenticationTokenClass(AccessTokenAuthToken.class);
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
        final User user = userService.load(accessToken.getUserName());
        if (user == null) {
            return null;
        }
        if (user.isExternalUser() && !ldapAuthenticator.isEnabled()) {
            throw new LockedAccountException("LDAP authentication is currently disabled.");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Found user {} for access token.", user);
        }
        try {
            accessTokenService.touch(accessToken);
        } catch (ValidationException e) {
            LOG.warn("Unable to update access token's last access date.", e);
        }
        return new SimpleAccount(user.getName(), null, "access token realm");
    }
}
