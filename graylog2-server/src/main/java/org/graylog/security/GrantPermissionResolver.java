package org.graylog.security;

import org.apache.shiro.authz.Permission;

import java.util.Set;

/**
 * Resolves a user name to specific permissions based on grants.
 */
public interface GrantPermissionResolver {
    /**
     * Returns resolved permissions for the given user name.
     *
     * @param userName the user name
     * @return the resolved permissions
     */
    Set<Permission> resolvePermissionsForUser(String userName);
}
