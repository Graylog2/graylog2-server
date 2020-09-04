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
package org.graylog.security.idp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@AutoValue
@JsonDeserialize(builder = UserProfile.Builder.class)
public abstract class UserProfile {
    public static final long DEFAULT_SESSION_TIMEOUT_MS = TimeUnit.HOURS.toMillis(8);

    public static final String FIELD_ID = "id";
    public static final String FIELD_UID = "uid";
    public static final String FIELD_IDP_BACKEND = "idp_backend";
    public static final String FIELD_IDP_GUID = "idp_guid";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_FULL_NAME = "full_name";
    public static final String FIELD_TIMEZONE = "timezone";
    public static final String FIELD_SESSION_TIMEOUT_MS = "session_timeout_ms";
    public static final String FIELD_PREFERENCES = "preferences";
    public static final String FIELD_START_PAGE = "start_page";
    public static final String FIELD_PERMISSIONS = "permissions";
    public static final String FIELD_ROLES = "roles";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String databaseId();

    @JsonProperty(FIELD_UID)
    public abstract String uid();

    @JsonProperty(FIELD_IDP_BACKEND)
    public abstract String idpBackend();

    @JsonProperty(FIELD_IDP_GUID)
    public abstract String idpGuid();

    @JsonProperty(FIELD_USERNAME)
    public abstract String username();

    @JsonProperty(FIELD_EMAIL)
    public abstract String email();

    @JsonProperty(FIELD_FULL_NAME)
    public abstract String fullName();

    @JsonProperty(FIELD_TIMEZONE)
    public abstract Optional<String> timezone();

    @JsonProperty(FIELD_SESSION_TIMEOUT_MS)
    public abstract long sessionTimeoutMs();

    @JsonProperty(FIELD_PREFERENCES)
    public abstract ImmutableMap<String, Object> preferences();

    @JsonProperty(FIELD_START_PAGE)
    public abstract Optional<StartPage> startPage();

    @JsonProperty(FIELD_PERMISSIONS)
    public abstract ImmutableSet<String> permissions();

    @JsonProperty(FIELD_ROLES)
    public abstract ImmutableSet<String> roles();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_UserProfile.Builder()
                    .sessionTimeoutMs(DEFAULT_SESSION_TIMEOUT_MS)
                    .preferences(Collections.emptyMap())
                    .permissions(Collections.emptySet())
                    .roles(Collections.emptySet());
        }

        @JsonProperty(FIELD_ID)
        public abstract Builder databaseId(String databaseId);

        @JsonProperty(FIELD_UID)
        public abstract Builder uid(String uid);

        @JsonProperty(FIELD_IDP_BACKEND)
        public abstract Builder idpBackend(String idpBackend);

        @JsonProperty(FIELD_IDP_GUID)
        public abstract Builder idpGuid(String idpGuid);

        @JsonProperty(FIELD_USERNAME)
        public abstract Builder username(String username);

        @JsonProperty(FIELD_EMAIL)
        public abstract Builder email(String email);

        @JsonProperty(FIELD_FULL_NAME)
        public abstract Builder fullName(String fullName);

        @JsonProperty(FIELD_TIMEZONE)
        public abstract Builder timezone(@Nullable String timezone);

        @JsonProperty(FIELD_SESSION_TIMEOUT_MS)
        public abstract Builder sessionTimeoutMs(long sessionTimeoutMs);

        @JsonProperty(FIELD_PREFERENCES)
        public abstract Builder preferences(Map<String, Object> preferences);

        @JsonProperty(FIELD_START_PAGE)
        public abstract Builder startPage(@Nullable StartPage startPage);

        @JsonProperty(FIELD_PERMISSIONS)
        public abstract Builder permissions(Set<String> permissions);

        @JsonProperty(FIELD_ROLES)
        public abstract Builder roles(Set<String> roles);

        public abstract UserProfile build();

    }

    @AutoValue
    public static abstract class StartPage {
        @JsonProperty("type")
        public abstract String type();

        @JsonProperty("id")
        public abstract String id();

        @JsonCreator
        public static StartPage create(@JsonProperty("type") String type, @JsonProperty("id") String id) {
            return new AutoValue_UserProfile_StartPage(type, id);
        }
    }
}
