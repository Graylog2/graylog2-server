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

import com.google.common.collect.Sets;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.graylog2.Core;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MongoDbAuthorizationRealm extends AuthorizingRealm {

    private static final Logger log = LoggerFactory.getLogger(MongoDbAuthorizationRealm.class);
    private final Core core;

    public MongoDbAuthorizationRealm(Core core) {
        this.core = core;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        log.debug("Retrieving authorization information for {}", principals);
        final User user = User.load(principals.getPrimaryPrincipal().toString(), core);
        final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        final List<String> permissions = user.getPermissions();
        if (permissions != null) {
            info.setStringPermissions(Sets.newHashSet(permissions));
        }
        log.debug("User {} has permissions: {}", principals, permissions);
        return info;
    }


    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // this class does not authenticate at all
        return null;
    }
}
