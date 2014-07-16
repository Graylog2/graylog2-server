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

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.jersey.container.netty.HeaderAwareSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.security.Principal;

/**
 * @author Kay Roepke <kay@torch.sh>
 */
public class ShiroSecurityContext implements HeaderAwareSecurityContext {
    private static final Logger log = LoggerFactory.getLogger(ShiroSecurityContext.class);

    private Subject subject;
    private final AuthenticationToken token;
    private final boolean secure;
    private final String authcScheme;
    private MultivaluedMap<String, String> headers;

    public ShiroSecurityContext(Subject subject, AuthenticationToken token, boolean isSecure, String authcScheme) {
        this.subject = subject;
        this.token = token;
        secure = isSecure;
        this.authcScheme = authcScheme;
    }

    public String getUsername() {
        if (token == null) {
            return null;
        }
        if (token.getPrincipal() == null) {
            return null;
        }
        return token.getPrincipal().toString();
    }

    public String getPassword() {
        if (token == null) {
            return null;
        }
        if (token.getCredentials() == null) {
            return null;
        }
        return token.getCredentials().toString();
    }

    public Subject getSubject() {
        return subject;
    }

    @Override
    public Principal getUserPrincipal() {
        return new ShiroPrincipal();
    }

    @Override
    public boolean isUserInRole(String role) {
        log.info("Checking role {} for user {}.", role, subject.getPrincipal());
        return subject.hasRole(role);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return authcScheme;
    }

    public void loginSubject() throws AuthenticationException {
        // what a hack :(
        ThreadContext.put("REQUEST_HEADERS", headers);
        subject.login(token);
        // the subject instance will change to include the session
        final Subject newSubject = ThreadContext.getSubject();
        if (newSubject != null) {
            subject = newSubject;
        }
        ThreadContext.remove("REQUEST_HEADERS");
    }

    @Override
    public void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }

    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    public class ShiroPrincipal implements Principal {
        @Override
        public String getName() {
            return subject.getPrincipal().toString();
        }

        public Subject getSubject() {
            return subject;
        }
    }
}
