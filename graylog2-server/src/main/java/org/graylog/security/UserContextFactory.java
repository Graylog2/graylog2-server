/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
