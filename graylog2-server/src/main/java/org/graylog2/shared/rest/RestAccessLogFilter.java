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

import org.glassfish.grizzly.http.server.Response;
import org.graylog2.rest.RestTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Date;

import static java.util.Objects.requireNonNull;

public class RestAccessLogFilter implements ContainerResponseFilter {
    private static final Logger LOG = LoggerFactory.getLogger("org.graylog2.rest.accesslog");

    private final Response response;
    private final RestTools resttools;

    @Inject
    public RestAccessLogFilter(@Context Response response, RestTools resttools) {
        this.response = requireNonNull(response);
        this.resttools = resttools;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        if (LOG.isDebugEnabled()) {
            try {
                final String rawQuery = requestContext.getUriInfo().getRequestUri().getRawQuery();
                final Date requestDate = requestContext.getDate();
                final String userName = RestTools.getUserNameFromRequest(requestContext);
                final String remoteAddress = resttools.getRemoteAddrFromRequest(response.getRequest());

                LOG.debug("{} {} [{}] \"{} {}{}\" {} {} {}",
                        remoteAddress,
                        userName == null ? "-" : userName,
                        (requestDate == null ? "-" : requestDate),
                        requestContext.getMethod(),
                        requestContext.getUriInfo().getPath(),
                        (rawQuery == null ? "" : "?" + rawQuery),
                        requestContext.getHeaderString(HttpHeaders.USER_AGENT),
                        responseContext.getStatus(),
                        responseContext.getLength());
            } catch (Exception e) {
                LOG.error("Error while processing REST API access log", e);
            }
        }
    }
}
