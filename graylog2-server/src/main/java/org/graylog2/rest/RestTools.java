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
package org.graylog2.rest;

import com.google.common.base.Strings;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.shared.security.ShiroPrincipal;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.graylog2.utilities.IpSubnet;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RestTools {

    @Nullable
    public static String getUserIdFromRequest(ContainerRequestContext requestContext) {
        final SecurityContext securityContext = requestContext.getSecurityContext();

        if (!(securityContext instanceof ShiroSecurityContext)) {
            return null;
        }

        final ShiroSecurityContext shiroSecurityContext = (ShiroSecurityContext) securityContext;
        final Principal userPrincipal = shiroSecurityContext.getUserPrincipal();

        if (!(userPrincipal instanceof ShiroPrincipal)) {
            return null;
        }

        final ShiroPrincipal shiroPrincipal = (ShiroPrincipal) userPrincipal;

        return shiroPrincipal.getName();
    }

    /**
     * If X-Forwarded-For request header is set, and the request came from a trusted source,
     * return the value of X-Forwarded-For. Otherwise return {@link Request#getRemoteAddr()}.
     */
    public static String getRemoteAddrFromRequest(Request request, Set<IpSubnet> trustedSubnets) {
        final String remoteAddr = request.getRemoteAddr();
        final String XForwardedFor = request.getHeader("X-Forwarded-For");
        if (XForwardedFor != null) {
            for (IpSubnet s : trustedSubnets) {
                try {
                    if (s.contains(remoteAddr)) {
                        // Request came from trusted source, trust X-Forwarded-For and return it
                        return XForwardedFor;
                    }
                } catch (UnknownHostException e) {
                    // ignore silently, probably not worth logging
                }
            }
        }

        // Request did not come from a trusted source, or the X-Forwarded-For header was not set
        return remoteAddr;
    }

    public static URI buildExternalUri(@NotNull MultivaluedMap<String, String> httpHeaders, @NotNull URI defaultUri) {
        Optional<URI> externalUri = Optional.empty();
        final List<String> headers = httpHeaders.get(HttpConfiguration.OVERRIDE_HEADER);
        if (headers != null && !headers.isEmpty()) {
            externalUri = headers.stream()
                    .filter(s -> {
                        try {
                            if (Strings.isNullOrEmpty(s)) {
                                return false;
                            }
                            final URI uri = new URI(s);
                            if (!uri.isAbsolute()) {
                                return true;
                            }
                            switch (uri.getScheme()) {
                                case "http":
                                case "https":
                                    return true;
                            }
                            return false;
                        } catch (URISyntaxException e) {
                            return false;
                        }
                    })
                    .map(URI::create)
                    .findFirst();
        }

        final URI uri = externalUri.orElse(defaultUri);

        // Make sure we return an URI object with a trailing slash
        if (!uri.toString().endsWith("/")) {
            return URI.create(uri.toString() + "/");
        }
        return uri;
    }

    public static String getPathFromResource(Resource resource) {
        String path = resource.getPath();
        Resource parent = resource.getParent();

        while (parent != null) {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            path = parent.getPath() + path;
            parent = parent.getParent();
        }

        return path;

    }
}
