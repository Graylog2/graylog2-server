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

import com.google.common.collect.Iterables;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

public class GrantsAuthorizingRealm extends AuthorizingRealm {
    public static final String NAME = "grants";

    private final GrantPermissionResolver grantPermissionResolver;
    private final GRNRegistry grnRegistry;

    @Inject
    public GrantsAuthorizingRealm(GrantPermissionResolver grantPermissionResolver, GRNRegistry grnRegistry) {
        this.grantPermissionResolver = grantPermissionResolver;
        this.grnRegistry = grnRegistry;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // This realm can handle both, user String principals and GRN principals.
        final GRN principal = getUserPrincipal(principals)
                .orElseGet(() -> getGRNPrincipal(principals).orElse(null));

        if (principal == null) {
            return new SimpleAuthorizationInfo();
        }

        final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        final Set<Permission> permissions = grantPermissionResolver.resolvePermissionsForPrincipal(principal);

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

    private Optional<GRN> getUserPrincipal(PrincipalCollection principals) {
        final String userName = Iterables.getFirst(principals.byType(String.class), null);
        if (isNullOrEmpty(userName)) {
            return Optional.empty();
        }
        return Optional.of(grnRegistry.newGRN("user", userName));
    }

    private Optional<GRN> getGRNPrincipal(PrincipalCollection principals) {
        final GRN principal = Iterables.getFirst(principals.byType(GRN.class), null);
        if (principal == null) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // This class does not authenticate at all
        return null;
    }
}
