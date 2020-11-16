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
package org.graylog.security;

import org.apache.shiro.subject.Subject;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ContainerRequest;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;


public class UserContextFactory implements Factory<UserContext> {

    // This needs to be done with a Provider, otherwise the injection happens
    // before the ShiroSecurityContextFilter can update the SecurityContext
    @Context
    private Provider<ContainerRequest> containerRequestProvider;

    private final UserService userService;

    @Inject
    public UserContextFactory(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserContext provide() {
        final SecurityContext securityContext = containerRequestProvider.get().getSecurityContext();
        if (securityContext instanceof ShiroSecurityContext) {
            final ShiroSecurityContext context = (ShiroSecurityContext) securityContext;
            final Subject subject = context.getSubject();
            return new UserContext.Factory(userService).create(subject);
        }
        throw new IllegalStateException("Failed to create UserContext");
    }

    @Override
    public void dispose(UserContext instance) {
    }
}
