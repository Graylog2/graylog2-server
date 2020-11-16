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
package org.graylog2.rest.models.roles.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.rest.models.users.responses.UserSummary;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collection;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class RoleMembershipResponse {

    @JsonProperty
    @NotBlank
    public abstract String role();

    @JsonProperty
    @NotNull
    public abstract Collection<UserSummary> users();

    @JsonCreator
    public static RoleMembershipResponse create(@JsonProperty("role") @NotBlank String roleName, @JsonProperty("users") @NotNull Collection<UserSummary> users) {
        return new AutoValue_RoleMembershipResponse(roleName, users);
    }
}
