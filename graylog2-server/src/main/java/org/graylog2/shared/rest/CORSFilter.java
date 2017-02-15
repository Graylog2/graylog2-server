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
package org.graylog2.shared.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class CORSFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // we have already added the necessary headers for OPTIONS requests below
        if ("options".equalsIgnoreCase(requestContext.getRequest().getMethod())) {
            if(Response.Status.Family.familyOf(responseContext.getStatus()) == Response.Status.Family.SUCCESSFUL) {
                return;
            }
            responseContext.setStatus(Response.Status.NO_CONTENT.getStatusCode());
            responseContext.setEntity("");
        }

        String origin = requestContext.getHeaders().getFirst("Origin");
        if (origin != null && !origin.isEmpty()) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", true);
            responseContext.getHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Graylog-No-Session-Extension, X-Requested-With");
            responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            // In order to avoid redoing the preflight thingy for every request, see http://stackoverflow.com/a/12021982/1088469
            responseContext.getHeaders().add("Access-Control-Max-Age", "600"); // 10 minutes seems to be the maximum allowable value
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // answer OPTIONS requests early so we don't have jersey produce WADL responses for them (we only use them for CORS preflight)
        if ("options".equalsIgnoreCase(requestContext.getRequest().getMethod())) {
            final Response.ResponseBuilder options = Response.noContent();
            String origin = requestContext.getHeaders().getFirst("Origin");
            if (origin != null && !origin.isEmpty()) {
                options.header("Access-Control-Allow-Origin", origin);
                options.header("Access-Control-Allow-Credentials", true);
                options.header("Access-Control-Allow-Headers",
                               "Authorization, Content-Type, X-Graylog-No-Session-Extension, X-Requested-With");
                options.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                // In order to avoid redoing the preflight thingy for every request, see http://stackoverflow.com/a/12021982/1088469
                options.header("Access-Control-Max-Age", "600"); // 10 minutes seems to be the maximum allowable value
                requestContext.abortWith(options.build());
            }
        }
    }
}
