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

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.glassfish.grizzly.http.server.Request;
import org.graylog2.rest.RestTools;
import org.graylog2.utilities.IpSubnet;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
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
import java.util.Set;

import static java.util.Objects.requireNonNull;

// Give this a higher priority so it's run before the authentication filter
@Priority(Priorities.AUTHENTICATION - 10)
public class ShiroSecurityContextFilter implements ContainerRequestFilter {
    private final DefaultSecurityManager securityManager;
    private Provider<Request> grizzlyRequestProvider;
    private final Set<IpSubnet> trustedProxies;

    @Inject
    public ShiroSecurityContextFilter(DefaultSecurityManager securityManager,
                                      Provider<Request> grizzlyRequestProvider,
                                      @Named("trusted_proxies") Set<IpSubnet> trustedProxies) {
        this.securityManager = requireNonNull(securityManager);
        this.grizzlyRequestProvider = grizzlyRequestProvider;
        this.trustedProxies = trustedProxies;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final boolean secure = requestContext.getSecurityContext().isSecure();
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        final Request grizzlyRequest = grizzlyRequestProvider.get();

        final String host = RestTools.getRemoteAddrFromRequest(grizzlyRequest, trustedProxies);
        final String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        final SecurityContext securityContext;
        if (authHeader != null && authHeader.startsWith("Basic")) {
            final String base64UserPass = authHeader.substring(authHeader.indexOf(' ') + 1);
            final String userPass = decodeBase64(base64UserPass);
            final String[] split = userPass.split(":", 2);

            if (split.length != 2) {
                throw new BadRequestException("Invalid credentials in Authorization header");
            }

            securityContext = createSecurityContext(split[0],
                                                    split[1],
                                                    secure,
                                                    SecurityContext.BASIC_AUTH,
                                                    host,
                                                    grizzlyRequest.getRemoteAddr(),
                                                    headers);

        } else {
            securityContext = createSecurityContext(null, null, secure, null, host,
                                                    grizzlyRequest.getRemoteAddr(),
                                                    headers);
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

    private SecurityContext createSecurityContext(String userName,
                                                  String credential,
                                                  boolean isSecure,
                                                  String authcScheme,
                                                  String host,
                                                  String remoteAddr,
                                                  MultivaluedMap<String, String> headers) {
        final AuthenticationToken authToken;
        if ("session".equalsIgnoreCase(credential)) {
            // Basic auth: undefined:session is sent when the UI doesn't have a valid session id,
            // we don't want to create a SessionIdToken in that case but fall back to looking at the headers instead
            if ("undefined".equalsIgnoreCase(userName)) {
                authToken = new HttpHeadersToken(headers, host, remoteAddr);
            } else {
                authToken = new SessionIdToken(userName, host);
            }
        } else if ("token".equalsIgnoreCase(credential)) {
            authToken = new AccessTokenAuthToken(userName, host);
        } else if (userName == null) { // without a username we default to using the header environment as potentially containing tokens used by plugins
            authToken = new HttpHeadersToken(headers, host, remoteAddr);
        } else { // otherwise we use the "standard" username/password combination
            authToken = new UsernamePasswordToken(userName, credential, host);
        }

        final Subject subject = new Subject.Builder(securityManager)
                .host(host)
                .sessionCreationEnabled(true)
                .buildSubject();

        return new ShiroSecurityContext(subject, authToken, isSecure, authcScheme, headers);
    }
}
