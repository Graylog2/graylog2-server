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

import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.util.Set;

public class RoleImpl implements Role {
    @Id
    @org.mongojack.ObjectId
    @JsonProperty("id")
    public String _id;

    @NotNull
    public String name;

    @NotNull
    public Set<String> permissions;

    @JsonProperty
    public String nameLower() {
        return name.toLowerCase();
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
    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
