/**
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
 */
package org.graylog2.rest;

import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.security.ShiroSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;

public class RestAccessLogFilter implements ContainerResponseFilter {
    private static final Logger LOG = LoggerFactory.getLogger("org.graylog2.rest.accesslog");
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (!LOG.isDebugEnabled()) return;
        try {
            final InetSocketAddress remoteAddr = (InetSocketAddress) requestContext.getProperty(NettyContainer.REQUEST_PROPERTY_REMOTE_ADDR);

            final String rawQuery = requestContext.getUriInfo().getRequestUri().getRawQuery();

            final String remoteUser = ((ShiroSecurityContext)requestContext.getSecurityContext()).getUsername();
            final Date requestDate = requestContext.getDate();
            LOG.debug("{} {} [{}] \"{} {}{}\" {} {} {}", new Object[]{
                    remoteAddr.getAddress().getHostAddress(),
                    (remoteUser == null ? "-" : remoteUser),
                    (requestDate == null ? "-" : requestDate),
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getPath(),
                    (rawQuery == null ? "" : "?" + rawQuery),
                    requestContext.getHeaderString(HttpHeaders.USER_AGENT),
                    responseContext.getStatus(),
                    responseContext.getLength()
            });
        } catch (Exception ignored) {
            LOG.error(":(", ignored);
        }
    }

}
