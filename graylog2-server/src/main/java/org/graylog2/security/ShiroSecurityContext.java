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
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * @author Kay Roepke <kay@torch.sh>
 */
public class ShiroSecurityContext implements SecurityContext {
    private static final Logger log = LoggerFactory.getLogger(ShiroSecurityContext.class);

    private final Subject subject;
    private final UsernamePasswordToken token;
    private final boolean secure;
    private final String authcScheme;

    public ShiroSecurityContext(Subject subject, UsernamePasswordToken token, boolean isSecure, String authcScheme) {
        this.subject = subject;
        this.token = token;
        secure = isSecure;
        this.authcScheme = authcScheme;
    }

    public Subject getSubject() {
        return subject;
    }

    @Override
    public Principal getUserPrincipal() {
        return new Principal() {
            @Override
            public String getName() {
                return subject.getPrincipal().toString();
            }
        };
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
        subject.login(token);
    }
}
