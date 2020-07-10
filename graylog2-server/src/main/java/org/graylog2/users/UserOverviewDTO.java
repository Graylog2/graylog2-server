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
package org.graylog2.users;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.security.MongoDbSession;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = UserOverviewDTO.Builder.class)
public abstract class UserOverviewDTO {

    private static final String FIELD_ID = "id";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_FULL_NAME = "full_name";
    private static final String FIELD_EXTERNAL_USER = "external_user";
    private static final String FIELD_ROLES = "roles";
    private static final String FIELD_READ_ONLY = "read_only";
    private static final String FIELD_SESSION_ACTIVE = "session_active";
    private static final String FIELD_LAST_ACTIVITY = "last_activity";
    private static final String FIELD_CLIENT_ADDRESS = "client_address";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_USERNAME)
    public abstract String username();

    @JsonProperty(FIELD_EMAIL)
    public abstract String email();

    @JsonProperty(FIELD_FULL_NAME)
    public abstract String fullName();

    @Nullable
    @JsonProperty(FIELD_EXTERNAL_USER)
    public abstract Boolean externalUser();

    @ObjectId
    @JsonProperty(FIELD_ROLES)
    public abstract Set<String> roles();

    @Nullable
    @JsonProperty(FIELD_READ_ONLY)
    public abstract Boolean readOnly();

    @Nullable
    @JsonProperty(FIELD_SESSION_ACTIVE)
    public abstract Boolean sessionActive();

    @Nullable
    @JsonProperty(FIELD_LAST_ACTIVITY)
    public abstract Date lastActivity();

    @Nullable
    @JsonProperty(FIELD_CLIENT_ADDRESS)
    public abstract String clientAddress();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonIgnoreProperties({ "preferences", "permissions", "timezone", "session_timeout_ms", "startpage", "password" })
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_UserOverviewDTO.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_USERNAME)
        public abstract Builder username(String username);

        @JsonProperty(FIELD_EMAIL)
        public abstract Builder email(String email);

        @JsonProperty(FIELD_FULL_NAME)
        public abstract Builder fullName(String fullName);

        @JsonProperty(FIELD_EXTERNAL_USER)
        public abstract Builder externalUser(@Nullable Boolean externalUser);

        @ObjectId
        @JsonProperty(FIELD_ROLES)
        public abstract Builder roles(@Nullable Set<String> roles);

        @JsonProperty(FIELD_READ_ONLY)
        public abstract Builder readOnly(@Nullable Boolean readOnly);

        @JsonProperty(FIELD_SESSION_ACTIVE)
        public abstract Builder sessionActive(@Nullable Boolean sessionActive);

        @JsonProperty(FIELD_LAST_ACTIVITY)
        public abstract Builder lastActivity(@Nullable Date lastActivity);

        @JsonProperty(FIELD_CLIENT_ADDRESS)
        public abstract Builder clientAddress(@Nullable String clientAddress);

        @JsonIgnore
        public Builder fillSession(Optional<MongoDbSession> session) {
           if (session.isPresent()) {
               MongoDbSession lastSession = session.get();
               return sessionActive(true)
                       .lastActivity(lastSession.getLastAccessTime())
                       .clientAddress(lastSession.getHost());
           };
           return this;
        }

        public abstract UserOverviewDTO build();
    }
}
