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
