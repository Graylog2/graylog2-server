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

import jakarta.annotation.Priority;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Priority(Priorities.AUTHENTICATION)
public class ShiroAuthenticationFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ShiroAuthenticationFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final SecurityContext securityContext = requestContext.getSecurityContext();
        if (securityContext instanceof ShiroSecurityContext) {
            final ShiroSecurityContext context = (ShiroSecurityContext) securityContext;
            final Subject subject = context.getSubject();

            LOG.trace("Authenticating... {}", subject);
            if (!subject.isAuthenticated()) {
                try {
                    LOG.trace("Logging in {}", subject);
                    context.loginSubject();
                } catch (LockedAccountException e) {
                    LOG.debug("Unable to authenticate user, account is locked.", e);
                    throw new NotAuthorizedException(e, "Basic realm=\"Graylog Server\"");
                } catch (AuthenticationException e) {
                    LOG.debug("Unable to authenticate user.", e);
                    throw new NotAuthorizedException(e, "Basic realm=\"Graylog Server\"");
                }
            }
        } else {
            throw new NotAuthorizedException("Basic realm=\"Graylog Server\"");
        }

    }
}
