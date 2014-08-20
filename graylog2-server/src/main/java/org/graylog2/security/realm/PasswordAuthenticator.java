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
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.util.ByteSource;
import org.graylog2.Configuration;
import org.graylog2.users.User;
import org.graylog2.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * @author Kay Roepke <kay@torch.sh>
 */
public class PasswordAuthenticator extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordAuthenticator.class);
    private final UserService userService;
    private final Configuration configuration;

    @Inject
    public PasswordAuthenticator(UserService userService, Configuration configuration) {
        this.userService = userService;
        this.configuration = configuration;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authToken;
        LOG.debug("Retrieving authc info for user {}", token.getUsername());

        final User user = userService.load(token.getUsername());
        if (user == null || user.isLocalAdmin()) {
            // skip the local admin user here, it's ugly, but for auth that user is treated specially.
            return null;
        }
        if (user.isExternalUser()) {
            // we don't store passwords for LDAP users, so we can't handle them here.
            LOG.trace("Skipping mongodb-based password check for LDAP user {}", token.getUsername());
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found user {} to be authenticated with password.", user.getName());
        }
        return new SimpleAccount(token.getPrincipal(),
                user.getHashedPassword(),
                ByteSource.Util.bytes(configuration.getPasswordSecret()),
                "graylog2MongoDbRealm");
    }
}
