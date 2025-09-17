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
import org.graylog2.shared.security.ShiroPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin wrapper around SecurityContext, providing similar helper methods for permission checks as RestResource does.
 * <p>
 * TODO: this should probably be extracted from {@link org.graylog2.shared.rest.resources.RestResource RestResource} instead
 */
public class PermissionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionHelper.class);

    private final SecurityContext securityContext;

    public PermissionHelper(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    protected Subject getSubject() {
        if (securityContext == null) {
            LOG.error("Cannot retrieve current subject, SecurityContext isn't set.");
            return null;
        }

        final Principal p = securityContext.getUserPrincipal();
        if (!(p instanceof ShiroPrincipal)) {
            final String msg = "Unknown SecurityContext class " + securityContext + ", cannot continue.";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        final ShiroPrincipal principal = (ShiroPrincipal) p;
        return principal.getSubject();
    }

    protected boolean isPermitted(String permission, String instanceId) {
        return getSubject().isPermitted(permission + ":" + instanceId);
    }

    protected void checkPermission(String permission) {
        if (!isPermitted(permission)) {
            LOG.info("Not authorized. User <{}> is missing permission <{}>", getSubject().getPrincipal(), permission);
            throw new ForbiddenException("Not authorized");
        }
    }

    protected boolean isPermitted(String permission) {
        return getSubject().isPermitted(permission);
    }

    protected void checkPermission(String permission, String instanceId) {
        if (!isPermitted(permission, instanceId)) {
            LOG.info("Not authorized to access resource id <{}>. User <{}> is missing permission <{}:{}>",
                    instanceId, getSubject().getPrincipal(), permission, instanceId);
            throw new ForbiddenException("Not authorized to access resource id <" + instanceId + ">");
        }
    }

    protected boolean isAnyPermitted(String[] permissions, final String instanceId) {
        final List<String> instancePermissions = Arrays.stream(permissions)
                .map(permission -> permission + ":" + instanceId)
                .collect(Collectors.toList());
        return isAnyPermitted(instancePermissions.toArray(new String[0]));
    }

    protected boolean isAnyPermitted(String... permissions) {
        final boolean[] permitted = getSubject().isPermitted(permissions);
        for (boolean p : permitted) {
            if (p) {
                return true;
            }
        }
        return false;
    }

    protected void checkAnyPermission(String permissions[], String instanceId) {
        if (!isAnyPermitted(permissions, instanceId)) {
            LOG.info("Not authorized to access resource id <{}>. User <{}> is missing permissions {} on instance <{}>",
                    instanceId, getSubject().getPrincipal(), Arrays.toString(permissions), instanceId);
            throw new ForbiddenException("Not authorized to access resource id <" + instanceId + ">");
        }
    }

}
