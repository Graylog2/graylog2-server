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
package org.graylog.plugins.views.search.rest.contexts;

import org.apache.shiro.subject.Subject;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.security.UserContext;
import org.graylog2.shared.security.ShiroPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Map;

public class SearchUserFactory implements Factory<SearchUser> {
    private static final Logger LOG = LoggerFactory.getLogger(SearchUserFactory.class);

    private final ServiceLocator serviceLocator;
    private final PermittedStreams permittedStreams;
    private final Map<String, ViewResolver> viewResolvers;

    @Inject
    public SearchUserFactory(ServiceLocator serviceLocator, PermittedStreams permittedStreams,
                             Map<String, ViewResolver> viewResolvers) {
        this.serviceLocator = serviceLocator;
        this.permittedStreams = permittedStreams;
        this.viewResolvers = viewResolvers;
    }

    @Override
    public SearchUser provide() {
        final UserContext userContext = serviceLocator.getService(UserContext.class);
        final SecurityContext securityContext = serviceLocator.getService(SecurityContext.class);
        return new SearchUser(userContext.getUser(), (permission) -> this.isPermitted(securityContext, permission),
                (permission, instanceId) -> this.isPermitted(securityContext, permission, instanceId), permittedStreams,
                viewResolvers);
    }

    protected Subject getSubject(SecurityContext securityContext) {
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

    protected boolean isPermitted(SecurityContext securityContext, String permission, String instanceId) {
        return getSubject(securityContext).isPermitted(permission + ":" + instanceId);
    }

    protected boolean isPermitted(SecurityContext securityContext, String permission) {
        return getSubject(securityContext).isPermitted(permission);
    }

    @Override
    public void dispose(SearchUser searchUser) {
    }
}
