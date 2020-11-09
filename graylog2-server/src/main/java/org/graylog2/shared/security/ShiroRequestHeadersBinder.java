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

import joptsimple.internal.Strings;
import org.apache.shiro.util.ThreadContext;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Optional;

/**
 * This filter makes the request headers accessible within Shiro's {@link ThreadContext}.
 */
// Needs to run after RequestIdFilter
@Priority(Priorities.AUTHORIZATION - 10)
public class ShiroRequestHeadersBinder implements ContainerRequestFilter {
    public static final String REQUEST_HEADERS = "REQUEST_HEADERS";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        ThreadContext.put(REQUEST_HEADERS, headers);
    }

    public static Optional<String> getHeaderFromThreadContext(String headerName) {
        @SuppressWarnings("unchecked")
        final MultivaluedMap<String, String> requestHeaders =
                (MultivaluedMap<String, String>) ThreadContext.get(REQUEST_HEADERS);
        if (requestHeaders != null) {
            final String header = requestHeaders.getFirst(headerName);
            if (!Strings.isNullOrEmpty(header)) {
                return Optional.of(header);
            }
        }
        return Optional.empty();
    }
}
