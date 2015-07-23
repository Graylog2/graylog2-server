package org.graylog2.rest.models.roles.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class RolesResponse {

    @JsonProperty
    public abstract Set<RoleResponse> roles();

    @JsonProperty
    public int getTotal() {
        return roles().size();
    }

    @JsonCreator
    public static RolesResponse create(@JsonProperty("roles") Set<RoleResponse> roles) {
        return new AutoValue_RolesResponse(roles);
    }
}
