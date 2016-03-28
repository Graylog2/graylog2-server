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

import org.graylog2.Configuration;
import org.graylog2.shared.security.ShiroPrincipal;
import org.graylog2.shared.security.ShiroSecurityContext;

import com.atlassian.ip.IPMatcher;

import org.glassfish.grizzly.http.server.Request;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class RestTools {

    private Configuration configuration;

    @Inject
    public RestTools(Configuration configuration) {
        this.configuration = configuration;
    }

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

    /**
     * If X-Forwarded-For request header is set, and the request came from a trusted source,
     * return the value of X-Forwarded-For. Otherwise return request.GetRemoteAddr();
     */
    public String getRemoteAddrFromRequest(Request request) {
        final String XForwardedFor = request.getHeader("X-Forwarded-For");
        IPMatcher ipmatcher = this.configuration.getTrustedProxies();

        if (XForwardedFor instanceof String && ipmatcher instanceof IPMatcher) {

            if (ipmatcher.matches(request.getRemoteAddr())) {
                // Request came from a trusted source, use X-Forwarded-For
                return XForwardedFor;
            }

        }

        // Request did not come from a trusted source, or the X-Forwarded-For header was not set
        return request.getRemoteAddr();
    }
}
