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
package org.graylog2.rest.models.system.sessions.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class SessionValidationResponse {
    @JsonProperty("is_valid")
    public abstract boolean isValid();

    @JsonProperty("session_id")
    @Nullable
    public abstract String sessionId();

    @JsonProperty("username")
    @Nullable
    public abstract String username();

    @JsonCreator
    public static SessionValidationResponse create(
            @JsonProperty("is_valid") boolean isValid,
            @JsonProperty("session_id") @Nullable String newSessionId,
            @JsonProperty("username") @Nullable String username) {
        return new AutoValue_SessionValidationResponse(isValid, newSessionId, username);
    }

    public static SessionValidationResponse valid() {
        return new AutoValue_SessionValidationResponse(true, null, null);
    }

    public static SessionValidationResponse validWithNewSession(String newSessionId, String username) {
        return new AutoValue_SessionValidationResponse(true, newSessionId, username);
    }

    public static SessionValidationResponse invalid() {
        return new AutoValue_SessionValidationResponse(false, null, null);
    }
}
