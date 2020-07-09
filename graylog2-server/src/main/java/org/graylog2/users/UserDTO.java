package org.graylog2.users;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.users.requests.Startpage;
import org.graylog2.security.MongoDbSession;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = UserDTO.Builder.class)
public abstract class UserDTO {

    private static final String FIELD_ID = "id";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_FULL_NAME = "full_name";
    private static final String FIELD_PERMISSIONS = "permissions";
    private static final String FIELD_PREFERENCES = "preferences";
    private static final String FIELD_TIMEZONE = "timezone";
    private static final String FIELD_EXTERNAL = "external";
    private static final String FIELD_SESSION_TIMEOUT_MS = "session_timeout_ms";
    private static final String FIELD_STARTPAGE = "startpage";
    private static final String FIELD_ROLES = "roles";
    private static final String FIELD_READ_ONLY = "read_only";
    private static final String FIELD_SESSION_ACTIVE = "session_active";
    private static final String FIELD_LAST_ACTIVITY = "last_activity";
    private static final String FIELD_CLIENT_ADDRESS = "client_address";
    private static final String FIELD_PASSWORD = "password";

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
    @JsonProperty(FIELD_PERMISSIONS)
    public abstract Set<String> permissions();

    @Nullable
    @JsonProperty(FIELD_PREFERENCES)
    public abstract Map<String, Object> preferences();

    @Nullable
    @JsonProperty(FIELD_TIMEZONE)
    public abstract DateTimeZone timezone();

    @Nullable
    @JsonProperty(FIELD_SESSION_TIMEOUT_MS)
    public abstract String sessionTimeoutMs();

    @Nullable
    @JsonProperty(FIELD_EXTERNAL)
    public abstract Boolean external();

    @Nullable
    @JsonProperty(FIELD_STARTPAGE)
    public abstract Startpage startpage();

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

    @JsonIgnore
    @Nullable
    @JsonProperty(FIELD_PASSWORD)
    public abstract String password();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_UserDTO.Builder();
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

        @JsonProperty(FIELD_PERMISSIONS)
        public abstract Builder permissions(@Nullable Set<String> permissions);

        @JsonProperty(FIELD_PREFERENCES)
        public abstract Builder preferences(@Nullable Map<String, Object> preferences);

        @JsonProperty(FIELD_TIMEZONE)
        public abstract Builder timezone(@Nullable DateTimeZone timezone);

        @JsonProperty(FIELD_SESSION_TIMEOUT_MS)
        public abstract Builder sessionTimeoutMs(@Nullable String sessionTimeoutMs);

        @JsonProperty(FIELD_EXTERNAL)
        public abstract Builder external(@Nullable Boolean external);

        @JsonProperty(FIELD_STARTPAGE)
        public abstract Builder startpage(@Nullable Startpage startpage);

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
        @JsonProperty(FIELD_PASSWORD)
        public abstract Builder password(@Nullable String password);

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

        public abstract UserDTO build();
    }
}
