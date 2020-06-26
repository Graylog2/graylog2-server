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

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // This class does not authenticate at all
        return null;
    }
}
