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
package org.graylog2.shared.security;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class ShiroSecurityContext implements SecurityContext {
    public static final String AUTO_CREATE_SESSION_KEY = "AUTO_CREATE_SESSION";
    private static final Logger LOG = LoggerFactory.getLogger(ShiroSecurityContext.class);

    private Subject subject;
    private final AuthenticationToken token;
    private final boolean secure;
    private final String authcScheme;
    private final MultivaluedMap<String, String> headers;

    public ShiroSecurityContext(Subject subject, AuthenticationToken token, boolean isSecure, String authcScheme,
                                MultivaluedMap<String, String> headers) {
        this.subject = subject;
        this.token = token;
        this.secure = isSecure;
        this.authcScheme = authcScheme;
        //noinspection Convert2Diamond
        this.headers = new MultivaluedHashMap<String, String>(headers);
    }

    public String getUsername() {
        if (token == null || token.getPrincipal() == null) {
            return null;
        }
        return token.getPrincipal().toString();
    }

    public String getPassword() {
        if (token == null || token.getCredentials() == null) {
            return null;
        }
        return token.getCredentials().toString();
    }

    public Subject getSubject() {
        return subject;
    }

    @Override
    public Principal getUserPrincipal() {
        return new ShiroPrincipal(subject);
    }

    @Override
    public boolean isUserInRole(String role) {
        LOG.debug("Checking role {} for user {}.", role, subject.getPrincipal());
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

    @VisibleForTesting
    public AuthenticationToken getToken() {
        return token;
    }

    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    public void loginSubject() throws AuthenticationException {
        subject.login(token);

        // the subject instance will change to include the session
        final Subject newSubject = ThreadContext.getSubject();
        if (newSubject != null) {
            subject = newSubject;
        }
    }

    public static boolean isSessionCreationRequested() {
        return Boolean.TRUE.equals(ThreadContext.get(AUTO_CREATE_SESSION_KEY));
    }

    public static void requestSessionCreation(boolean createSessionRequest) {
        ThreadContext.put(AUTO_CREATE_SESSION_KEY, createSessionRequest);
    }
}