/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
