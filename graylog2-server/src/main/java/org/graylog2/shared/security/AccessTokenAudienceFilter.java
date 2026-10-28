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

import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Rejects requests made with audience-restricted access tokens to resources outside of that audience.
 */
@Priority(Priorities.AUTHENTICATION + 100)
public class AccessTokenAudienceFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenAudienceFilter.class);

    @Nullable
    private final String resourceAudience;

    public AccessTokenAudienceFilter(@Nullable String resourceAudience) {
        this.resourceAudience = resourceAudience;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!(requestContext.getSecurityContext() instanceof ShiroSecurityContext context)) {
            return;
        }
        final PrincipalCollection principals = context.getSubject().getPrincipals();
        if (principals == null) {
            return;
        }
        final AccessTokenPrincipal tokenPrincipal = principals.oneByType(AccessTokenPrincipal.class);
        if (tokenPrincipal == null || tokenPrincipal.audience() == null) {
            return;
        }
        if (!Objects.equals(tokenPrincipal.audience(), resourceAudience)) {
            LOG.debug("Access token <{}> with audience <{}> is not allowed to access <{}>",
                    tokenPrincipal.tokenId(), tokenPrincipal.audience(), requestContext.getUriInfo().getPath());
            throw new ForbiddenException("Access token is not valid for this resource");
        }
    }
}
