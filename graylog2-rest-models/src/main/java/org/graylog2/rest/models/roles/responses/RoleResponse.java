package org.graylog2.rest.models.roles.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class RoleResponse {

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract Set<String> permissions();

    @JsonCreator
    public static RoleResponse create(@JsonProperty("name") String name, @JsonProperty("permissions") Set<String> permissions) {
        return new AutoValue_RoleResponse(name, permissions);
    }
}
