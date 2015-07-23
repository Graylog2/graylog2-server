package org.graylog2.rest.models.roles.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.users.responses.UserSummary;

import java.util.Collection;

@AutoValue
public abstract class RoleMembershipResponse {

    @JsonProperty
    public abstract String role();

    @JsonProperty
    public abstract Collection<UserSummary> users();

    @JsonCreator
    public static RoleMembershipResponse create(@JsonProperty("role") String roleName, @JsonProperty("users") Collection<UserSummary> users) {
        return new AutoValue_RoleMembershipResponse(roleName, users);
    }
}
