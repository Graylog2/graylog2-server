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
package org.graylog2.rest.resources.users.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class User {

    @JsonProperty
    @Nullable
    public abstract String id();

    @JsonProperty
    public abstract String username();

    @JsonProperty
    public abstract String email();

    @JsonProperty
    public abstract String fullName();

    @JsonProperty
    public abstract List<String> permissions();

    @JsonProperty
    @Nullable
    public abstract Map<String, Object> preferences();

    @JsonProperty
    @Nullable
    public abstract String timezone();

    @JsonProperty
    @Nullable
    public abstract Long sessionTimeoutMs();

    @JsonProperty
    public abstract boolean readOnly();

    @JsonProperty
    public abstract boolean external();

    @JsonProperty
    @Nullable
    public abstract Map<String, String> startpage();

    public static User create(@Nullable String id,
                              String username,
                              String email,
                              @Nullable String fullName,
                              @Nullable List<String> permissions,
                              @Nullable Map<String, Object> preferences,
                              @Nullable String timezone,
                              @Nullable Long sessionTimeoutMs,
                              boolean readOnly,
                              boolean external,
                              @Nullable Map<String, String> startpage) {
        return new AutoValue_User(id, username, email, fullName, permissions, preferences, timezone, sessionTimeoutMs, readOnly, external, startpage);
    }
}
