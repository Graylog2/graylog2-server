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
package org.graylog2.rest.models.users.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CreateUserRequest {

    @JsonProperty
    public abstract String username();

    @JsonProperty
    public abstract String password();

    @JsonProperty
    public abstract String email();

    @JsonProperty
    public abstract String firstName();

    @JsonProperty
    public abstract String lastName();

    @JsonProperty
    public abstract String fullName();

    @JsonProperty
    public abstract List<String> permissions();

    @JsonProperty
    @Nullable
    public abstract String timezone();

    @JsonProperty
    @Nullable
    public abstract Long sessionTimeoutMs();

    @JsonProperty
    @Nullable
    public abstract Startpage startpage();

    @JsonProperty
    @Nullable
    public abstract List<String> roles();

    @JsonCreator
    public static CreateUserRequest create(@JsonProperty("username") @NotEmpty String username,
                                           @JsonProperty("password") @NotEmpty String password,
                                           @JsonProperty("email") @Email String email,
                                           @JsonProperty("first_name") @NotEmpty String firstName,
                                           @JsonProperty("last_name") @NotEmpty String lastName,
                                           @JsonProperty("full_name") @Nullable String fullName,
                                           @JsonProperty("permissions") @NotNull List<String> permissions,
                                           @JsonProperty("timezone") @Nullable String timezone,
                                           @JsonProperty("session_timeout_ms") @Nullable @Min(1) Long sessionTimeoutMs,
                                           @JsonProperty("startpage") @Nullable Startpage startpage,
                                           @JsonProperty("roles") @Nullable List<String> roles) {
        return new AutoValue_CreateUserRequest(username, password, email, firstName, lastName,
                                               fullName, permissions, timezone, sessionTimeoutMs, startpage, roles);
    }
}
