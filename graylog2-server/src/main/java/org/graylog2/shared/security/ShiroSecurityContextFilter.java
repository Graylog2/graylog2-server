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

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static java.util.Objects.requireNonNull;

@Priority(Priorities.AUTHENTICATION)
public class ShiroSecurityContextFilter implements ContainerRequestFilter {
    private final DefaultSecurityManager securityManager;

    @Inject
    public ShiroSecurityContextFilter(DefaultSecurityManager securityManager) {
        this.securityManager = requireNonNull(securityManager);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final boolean secure = requestContext.getSecurityContext().isSecure();
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        final String host = headers.getFirst(HttpHeaders.HOST);
        final String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        final SecurityContext securityContext;
        if (authHeader != null && authHeader.startsWith("Basic")) {
            final String base64UserPass = authHeader.substring(authHeader.indexOf(' ') + 1);
            final String userPass = decodeBase64(base64UserPass);
            final String[] split = userPass.split(":");

            if (split.length != 2) {
                throw new BadRequestException("Invalid credentials in Authorization header");
            }

            securityContext = createSecurityContext(split[0], split[1], secure, SecurityContext.BASIC_AUTH, host, headers);

        } else {
            securityContext = createSecurityContext(null, null, secure, null, host, headers);
        }

        requestContext.setSecurityContext(securityContext);
    }

    private String decodeBase64(String s) {
        try {
            return new String(Base64.getDecoder().decode(s), StandardCharsets.US_ASCII);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private SecurityContext createSecurityContext(String userName, String credential, boolean isSecure, String authcScheme, String host,
                                                  MultivaluedMap<String, String> headers) {
        final AuthenticationToken authToken;
        if ("session".equalsIgnoreCase(credential)) {
            authToken = new SessionIdToken(userName, host);
        } else if ("token".equalsIgnoreCase(credential)) {
            authToken = new AccessTokenAuthToken(userName, host);
        } else {
            authToken = new UsernamePasswordToken(userName, credential, host);
        }

        final Subject subject = new Subject.Builder(securityManager)
                .host(host)
                .sessionCreationEnabled(false)
                .buildSubject();

        return new ShiroSecurityContext(subject, authToken, isSecure, authcScheme, headers);
    }
}
