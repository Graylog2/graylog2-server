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
package org.graylog.security.authzroles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.shared.users.Role;
import org.graylog2.users.RoleImpl;
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
    @Nullable
    public abstract String description();

    @JsonProperty(FIELD_PERMISSIONS)
    public abstract Set<String> permissions();

    @JsonProperty(FIELD_READ_ONLY)
    public abstract boolean readOnly();

    public Role toLegacyRole() {
        final RoleImpl legacyRole = new RoleImpl();
        legacyRole._id = id();
        legacyRole.setName(name());
        legacyRole.setDescription(description());
        legacyRole.setPermissions(permissions());
        legacyRole.setReadOnly(readOnly());
        return legacyRole;
    }

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
        public abstract Builder description(@Nullable String description);

        @JsonProperty(FIELD_PERMISSIONS)
        public abstract Builder permissions(Set<String> permissions);

        @JsonProperty(FIELD_READ_ONLY)
        public abstract Builder readOnly(boolean readOnly);

        public abstract AuthzRoleDTO build();
    }
}
