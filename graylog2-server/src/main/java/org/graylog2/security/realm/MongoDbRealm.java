/**
 * Copyright 2013 Kay Roepke <kay@torch.sh>
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
 *
 */
package org.graylog2.security.realm;

import com.google.common.collect.Sets;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.graylog2.Core;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Kay Roepke <kay@torch.sh>
 */
public class MongoDbRealm extends AuthorizingRealm {
    private static final Logger log = LoggerFactory.getLogger(MongoDbRealm.class);
    private final Core core;

    public MongoDbRealm(Core core) {
        this.core = core;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        log.trace("Retrieving authz information for {}", principals);
        final User user = User.load(principals.getPrimaryPrincipal().toString(), core);
        final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        final List<String> permissions = user.getPermissions();
        if (permissions != null) {
            info.setStringPermissions(Sets.newHashSet(permissions));
        }
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        if (!(authToken instanceof UsernamePasswordToken)) {
            throw new IllegalArgumentException("Only implemented for UsernamePasswordToken currently.");
        }
        UsernamePasswordToken token = (UsernamePasswordToken) authToken;
        log.info("Retrieving authc info for user {}", token.getUsername());

        final SimpleAccount simpleAccount;
        if (User.exists(token.getUsername(), new String(token.getPassword()), core)) {
            simpleAccount = new SimpleAccount(token.getPrincipal(),
                    token.getCredentials(),
                    ByteSource.Util.bytes(core.getConfiguration().getPasswordSecret()),
                    "graylog2MongoDbRealm");
            log.info("User {} authenticated by hashed password", token.getUsername());
        } else {
            log.warn("User {} could not be authenticated", token.getUsername());
            throw new AuthenticationException("Unknown user or wrong credentials.");
        }

        return simpleAccount;
    }
}
