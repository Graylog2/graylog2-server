package org.graylog.security.authzroles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = AuthzRoleDTO.Builder.class)
public abstract class AuthzRoleDTO {

    private static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_PERMISSIONS = "permissions";
    private static final String FIELD_READ_ONLY = "read_only";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_PERMISSIONS)
    public abstract Set<String> permissions();

    @JsonProperty(FIELD_READ_ONLY)
    public abstract boolean readOnly();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonIgnoreProperties({"name_lower"})
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_AuthzRoleDTO.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_NAME)
        public abstract Builder name(String name);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_PERMISSIONS)
        public abstract Builder permissions(Set<String> permissions);

        @JsonProperty(FIELD_READ_ONLY)
        public abstract Builder readOnly(boolean readOnly);

        public abstract AuthzRoleDTO build();
    }
}

