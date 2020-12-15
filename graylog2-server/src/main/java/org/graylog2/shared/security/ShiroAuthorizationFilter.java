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
package org.graylog2.shared.security;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.rest.RestTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Arrays;

@Priority(Priorities.AUTHORIZATION)
public class ShiroAuthorizationFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ShiroAuthorizationFilter.class);
    private final RequiresPermissions annotation;

    public ShiroAuthorizationFilter(RequiresPermissions annotation) {
        this.annotation = annotation;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final SecurityContext securityContext = requestContext.getSecurityContext();
        if (securityContext instanceof ShiroSecurityContext) {
            final ShiroSecurityContext context = (ShiroSecurityContext) securityContext;
            final String userId = RestTools.getUserIdFromRequest(requestContext);
            final ContextAwarePermissionAnnotationHandler annotationHandler = new ContextAwarePermissionAnnotationHandler(context);
            final String[] requiredPermissions = annotation.value();
            try {
                LOG.debug("Checking authorization for user [{}], needs permissions: {}", userId, requiredPermissions);
                annotationHandler.assertAuthorized(annotation);
            } catch (AuthorizationException e) {
                LOG.info("Not authorized. User <{}> is missing permissions {} to perform <{} {}>",
                        userId, Arrays.toString(requiredPermissions), requestContext.getMethod(), requestContext.getUriInfo().getPath());
                throw new ForbiddenException("Not authorized");
            }
        } else {
            throw new ForbiddenException();
        }
    }
}
