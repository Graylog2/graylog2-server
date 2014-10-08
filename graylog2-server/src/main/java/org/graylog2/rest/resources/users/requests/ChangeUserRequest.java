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
package org.graylog2.rest.resources.users.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class ChangeUserRequest {
    @JsonProperty
    @Nullable
    public abstract String email();

    @JsonProperty
    @Nullable
    public abstract String fullName();

    @JsonProperty
    @Nullable
    public abstract List<String> permissions();

    @JsonProperty
    @Nullable
    public abstract String timezone();

    @JsonProperty
    @Nullable
    public abstract Startpage startpage();

    @JsonProperty
    @Nullable
    public abstract Long sessionTimeoutMs();

    @JsonCreator
    public static ChangeUserRequest create(@JsonProperty("email") @Nullable @Email String email,
                                           @JsonProperty("full_name") @Nullable String fullName,
                                           @JsonProperty("permissions") @Nullable List<String> permissions,
                                           @JsonProperty("timezone") @Nullable String timezone,
                                           @JsonProperty("startpage") @Nullable @Valid Startpage startpage,
                                           @JsonProperty("session_timeout_ms") @Nullable @Min(1) Long sessionTimeoutMs) {
        return new AutoValue_ChangeUserRequest(email, fullName, permissions, timezone, startpage, sessionTimeoutMs);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class Startpage {
        @JsonProperty
        public abstract String type();

        @JsonProperty
        public abstract String id();

        @JsonCreator
        public static Startpage create(@JsonProperty("type") @NotEmpty String type,
                                       @JsonProperty("id") @NotEmpty String id) {
            return new AutoValue_ChangeUserRequest_Startpage(type, id);
        }
    }
}