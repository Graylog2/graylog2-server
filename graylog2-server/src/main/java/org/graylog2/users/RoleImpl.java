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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.shared.users.Role;
import org.mongojack.Id;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class RoleImpl implements Role {
    @Id
    @org.mongojack.ObjectId
    @JsonProperty("id")
    public String _id;

    @NotNull
    public String name;

    @NotNull
    public Set<String> permissions;

    @Nullable
    private String description;

    // readOnly is never set from the outside, except for the two built-in roles "Admin" and "Reader"
    private boolean readOnly = false;

    @JsonProperty
    public String nameLower() {
        return name.toLowerCase(Locale.ENGLISH);
    }

    @JsonProperty
    public void setNameLower(String ignored) {
        // ignored
    }

    @Override
    @JsonIgnore
    public String getId() {
        return _id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonProperty
    @Nullable
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Override
    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    @JsonProperty
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleImpl role = (RoleImpl) o;
        return readOnly == role.readOnly &&
                Objects.equals(_id, role._id) &&
                Objects.equals(name, role.name) &&
                Objects.equals(permissions, role.permissions) &&
                Objects.equals(description, role.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, name, permissions, description, readOnly);
    }
}
