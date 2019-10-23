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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ActorAwareUsernamePasswordTokenFactory implements ActorAwareAuthenticationTokenFactory {

    @Override
    public ActorAwareAuthenticationToken forRequestBody(JsonNode jsonBody) {

        String missingProperties = Stream.of("username", "password")
                .filter(key -> jsonBody.get(key) == null || jsonBody.get(key).asText().isEmpty())
                .collect(Collectors.joining(", "));

        if (!missingProperties.isEmpty()) {
            throw new IllegalArgumentException("Missing required properties: " + missingProperties + ".");
        }

        String username = jsonBody.get("username").asText();
        String password = jsonBody.get("password").asText();

        return new ActorAwareUsernamePasswordToken(username, password);
    }
}
