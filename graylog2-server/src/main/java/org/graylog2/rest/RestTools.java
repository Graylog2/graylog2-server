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
package org.graylog2.rest;

import com.google.common.base.Strings;
import org.graylog2.shared.security.ShiroPrincipal;
import org.graylog2.shared.security.ShiroSecurityContext;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

public class RestTools {
    @Nullable
    public static String getUserNameFromRequest(ContainerRequestContext requestContext) {
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

    public static String buildEndpointUri(@NotNull HttpHeaders httpHeaders, @NotNull URI defaultEndpointUri) {
        Optional<String> endpointUri = Optional.empty();
        final List<String> headers = httpHeaders.getRequestHeader("X-Graylog-Server-URL");
        if (headers != null && !headers.isEmpty()) {
            endpointUri = headers.stream().filter(s -> {
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
            }).findFirst();
        }

        return endpointUri.orElse(defaultEndpointUri.toString());
    }
}
