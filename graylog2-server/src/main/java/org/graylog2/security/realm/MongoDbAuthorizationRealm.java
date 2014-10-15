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

import com.google.common.collect.Sets;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.graylog2.users.User;
import org.graylog2.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class MongoDbAuthorizationRealm extends AuthorizingRealm {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDbAuthorizationRealm.class);
    private final UserService userService;

    @Inject
    public MongoDbAuthorizationRealm(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        LOG.debug("Retrieving authorization information for {}", principals);
        final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        final User user = userService.load(principals.getPrimaryPrincipal().toString());

        final List<String> permissions;
        if (null == user) {
            permissions = Collections.emptyList();
        } else {
            permissions = user.getPermissions();

            if (permissions != null) {
                info.setStringPermissions(Sets.newHashSet(permissions));
            }

        }

        LOG.debug("User {} has permissions: {}", principals, permissions);
        return info;
    }


    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // this class does not authenticate at all
        return null;
    }
}
