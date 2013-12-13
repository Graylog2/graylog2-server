/**
 * Copyright 2013 Kay Roepke <kay@torch.sh>
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
 *
 */
package org.graylog2.security;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;

public class ShiroAuthorizationFilter implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ShiroAuthorizationFilter.class);
    private final RequiresPermissions annotation;

    public ShiroAuthorizationFilter(RequiresPermissions annotation) {
        this.annotation = annotation;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final SecurityContext securityContext = requestContext.getSecurityContext();
        if (!(securityContext instanceof ShiroSecurityContext)) {
            return;
        }
        final ShiroSecurityContext context = (ShiroSecurityContext) securityContext;
        final Subject subject = context.getSubject();
        try {
            log.debug("Checking authorization for user {}, needs permissions {}", subject, annotation.value());
            new ContextAwarePermissionAnnotationHandler(context).assertAuthorized(annotation);
        } catch (AuthorizationException e) {
            log.info("User not authorized.", e);
            throw new NotAuthorizedException(e, "Basic", "Graylog2 Server");
        }
    }
}
