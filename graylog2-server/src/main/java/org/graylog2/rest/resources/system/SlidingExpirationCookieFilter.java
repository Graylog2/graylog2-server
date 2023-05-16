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
package org.graylog2.rest.resources.system;

import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.graylog2.rest.models.system.sessions.responses.SessionResponseFactory;
import org.graylog2.shared.security.ShiroPrincipal;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Optional;

import static org.graylog2.security.realm.SessionAuthenticator.X_GRAYLOG_NO_SESSION_EXTENSION;

public class SlidingExpirationCookieFilter implements ContainerResponseFilter {
    private final CookieFactory cookieFactory;
    private final SessionResponseFactory sessionResponseFactory;

    @Inject
    public SlidingExpirationCookieFilter(CookieFactory cookieFactory, SessionResponseFactory sessionResponseFactory) {
        this.cookieFactory = cookieFactory;
        this.sessionResponseFactory = sessionResponseFactory;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (noSessionExtension(requestContext)) {
            return;
        }

        sessionFromRequest(requestContext).ifPresent(session -> {
            var response = sessionResponseFactory.forSession(session);
            var cookie = cookieFactory.createAuthenticationCookie(response, requestContext);

            setCookie(responseContext, cookie);
        });
    }

    private Optional<Session> sessionFromRequest(ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getSecurityContext())
                .map(SecurityContext::getUserPrincipal)
                .filter(p -> p instanceof ShiroPrincipal)
                .map(principal -> ((ShiroPrincipal) principal).getSubject())
                .filter(Subject::isAuthenticated)
                .map(subject -> subject.getSession(false));
    }

    private boolean noSessionExtension(ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getHeaderString(X_GRAYLOG_NO_SESSION_EXTENSION))
                .map("true"::equalsIgnoreCase)
                .orElse(false);
    }

    private void setCookie(ContainerResponseContext responseContext, NewCookie cookie) {
        responseContext.getHeaders().add("Set-Cookie", cookie);
    }
}
