/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.mcp.tools;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.ShiroPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

/**
 * Thin wrapper around SecurityContext, providing similar helper methods for permission checks as RestResource does.
 * <p>
 * TODO: this should probably be extracted from {@link org.graylog2.shared.rest.resources.RestResource RestResource} instead
 */
public class PermissionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionHelper.class);

    private final User currentUser;
    private final SecurityContext securityContext;
    private final SearchUser searchUser;

    public PermissionHelper(final User currentUser, SecurityContext securityContext, final SearchUser searchUser) {
        this.currentUser = currentUser;
        this.securityContext = securityContext;
        this.searchUser = searchUser;
    }

    public Subject getSubject() {
        if (securityContext == null) {
            LOG.error("Cannot retrieve current subject, SecurityContext isn't set.");
            return null;
        }

        final Principal p = securityContext.getUserPrincipal();
        if (!(p instanceof final ShiroPrincipal principal)) {
            final String msg = "Unknown SecurityContext class " + securityContext + ", cannot continue.";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        return principal.getSubject();
    }

    public boolean isPermitted(String permission, String instanceId) {
        return getSubject().isPermitted(permission + ":" + instanceId);
    }

    public void checkPermission(String permission) {
        if (!isPermitted(permission)) {
            LOG.info("Not authorized. User <{}> is missing permission <{}>", getSubject().getPrincipal(), permission, new Throwable());
            throw new ForbiddenException("Not authorized");
        }
    }

    public boolean isPermitted(String permission) {
        return getSubject().isPermitted(permission);
    }

    public void checkPermission(String permission, String instanceId) {
        if (!isPermitted(permission, instanceId)) {
            LOG.info("Not authorized to access resource id <{}>. User <{}> is missing permission <{}:{}>",
                    instanceId, getSubject().getPrincipal(), permission, instanceId);
            throw new ForbiddenException("Not authorized to access resource id <" + instanceId + ">");
        }
    }

    public boolean isAnyPermitted(String[] permissions, final String instanceId) {
        final List<String> instancePermissions = Arrays.stream(permissions)
                .map(permission -> permission + ":" + instanceId)
                .toList();
        return isAnyPermitted(instancePermissions.toArray(new String[0]));
    }

    public boolean isAnyPermitted(String... permissions) {
        final boolean[] permitted = getSubject().isPermitted(permissions);
        for (boolean p : permitted) {
            if (p) {
                return true;
            }
        }
        return false;
    }

    public void checkAnyPermission(String[] permissions, String instanceId) {
        if (!isAnyPermitted(permissions, instanceId)) {
            LOG.info("Not authorized to access resource id <{}>. User <{}> is missing permissions {} on instance <{}>",
                    instanceId, getSubject().getPrincipal(), Arrays.toString(permissions), instanceId);
            throw new ForbiddenException("Not authorized to access resource id <" + instanceId + ">");
        }
    }

    public SearchUser getSearchUser() {
        return searchUser;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
