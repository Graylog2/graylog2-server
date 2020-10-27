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

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// Needs to run before ShiroAuthorizationFilter
@Priority(Priorities.AUTHORIZATION - 20)
public class RequestIdFilter implements ContainerRequestFilter {
    public final static String X_REQUEST_ID = "X-Request-Id";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String id = requestContext.getHeaderString(X_REQUEST_ID);
        if (Strings.isNullOrEmpty(id)) {
            id = generateRandomUuid().toString();
        }
        requestContext.getHeaders().putSingle(X_REQUEST_ID, id);
    }

    /**
     * Generate a random UUID v4 that will perform reasonably when used by
     * multiple threads under load.
     *
     * @see <a href="https://github.com/Netflix/netflix-commons/blob/v0.3.0/netflix-commons-util/src/main/java/com/netflix/util/concurrent/ConcurrentUUIDFactory.java">ConcurrentUUIDFactory</a>
     * @return random UUID
     */
    private static UUID generateRandomUuid() {
        final Random rnd = ThreadLocalRandom.current();
        long mostSig  = rnd.nextLong();
        long leastSig = rnd.nextLong();

        // Identify this as a version 4 UUID, that is one based on a random value.
        mostSig &= 0xffffffffffff0fffL;
        mostSig |= 0x0000000000004000L;

        // Set the variant identifier as specified for version 4 UUID values.  The two
        // high order bits of the lower word are required to be one and zero, respectively.
        leastSig &= 0x3fffffffffffffffL;
        leastSig |= 0x8000000000000000L;

        return new UUID(mostSig, leastSig);
    }
}
