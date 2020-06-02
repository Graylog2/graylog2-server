/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security.realm;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.MongoDbAuthorizationCacheManager;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.events.UserChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public class MongoDbAuthorizationRealm extends AuthorizingRealm {

    public static final String NAME = "mongodb-authorization-realm";
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbAuthorizationRealm.class);
    private final UserService userService;

    @Inject
    MongoDbAuthorizationRealm(UserService userService,
                              MongoDbAuthorizationCacheManager mongoDbAuthorizationCacheManager,
                              EventBus serverEventBus) {
        this.userService = userService;
        setCachingEnabled(true);
        setCacheManager(mongoDbAuthorizationCacheManager);
        serverEventBus.register(this);
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        LOG.debug("Retrieving authorization information for {}", principals);
        final User user = userService.load(principals.getPrimaryPrincipal().toString());

        if (user == null) {
            return new SimpleAuthorizationInfo();
        } else {
            final SimpleAuthorizationInfo info = new UserAuthorizationInfo(user);
            final List<String> permissions = user.getPermissions();

            if (permissions != null) {
                info.setStringPermissions(Sets.newHashSet(permissions));
            }
            info.setRoles(user.getRoleIds());

            LOG.debug("User {} has permissions: {}", principals, permissions);
            return info;
        }
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // this class does not authenticate at all
        return null;
    }

    @Subscribe
    public void handleUserSave(UserChangedEvent event) {
        getAuthorizationCache().clear();
    }
}
