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

import com.google.common.base.Strings;
import org.graylog.util.uuid.ConcurrentUUID;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

// Needs to run before ShiroAuthorizationFilter
@Priority(Priorities.AUTHORIZATION - 20)
public class RequestIdFilter implements ContainerRequestFilter {
    public final static String X_REQUEST_ID = "X-Request-Id";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String id = requestContext.getHeaderString(X_REQUEST_ID);
        if (Strings.isNullOrEmpty(id)) {
            id = ConcurrentUUID.generateRandomUuid().toString();
        }
        requestContext.getHeaders().putSingle(X_REQUEST_ID, id);
    }
}
