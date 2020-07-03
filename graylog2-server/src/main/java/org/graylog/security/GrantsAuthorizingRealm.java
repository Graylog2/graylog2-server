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
package org.graylog.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import javax.inject.Inject;
import java.util.Set;

public class GrantsAuthorizingRealm extends AuthorizingRealm {
    public static final String NAME = "grants";

    private final GrantPermissionResolver grantPermissionResolver;

    @Inject
    public GrantsAuthorizingRealm(GrantPermissionResolver grantPermissionResolver) {
        this.grantPermissionResolver = grantPermissionResolver;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        final String userName = principals.getPrimaryPrincipal().toString();

        final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        final Set<Permission> permissions = grantPermissionResolver.resolvePermissionsForUser(userName);

        if (!permissions.isEmpty()) {
            info.setObjectPermissions(permissions);
        }

        return info;
    }

    // This class does not authenticate at all
    @Override
    public boolean supports(AuthenticationToken token) {
        return false;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // This class does not authenticate at all
        return null;
    }
}
