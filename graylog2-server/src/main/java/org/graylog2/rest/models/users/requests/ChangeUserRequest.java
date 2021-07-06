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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ChangeUserRequest {
    @JsonProperty
    @Nullable
    public abstract String email();

    @JsonProperty
    @Nullable
    public abstract String firstName();

    @JsonProperty
    @Nullable
    public abstract String lastName();

    @JsonProperty
    @Nullable
    public abstract List<String> permissions();

    @JsonProperty
    @Nullable
    public abstract String timezone();

    @JsonProperty
    @Nullable
    public abstract Startpage startpage();

    @JsonProperty
    @Nullable
    public abstract Long sessionTimeoutMs();

    @JsonProperty
    @Nullable
    public abstract List<String> roles();

    @JsonCreator
    public static ChangeUserRequest create(@JsonProperty("email") @Nullable @Email String email,
                                           @JsonProperty("first_name") @Nullable String firstName,
                                           @JsonProperty("last_name") @Nullable String lastName,
                                           @JsonProperty("permissions") @Nullable List<String> permissions,
                                           @JsonProperty("timezone") @Nullable String timezone,
                                           @JsonProperty("startpage") @Nullable @Valid Startpage startpage,
                                           @JsonProperty("session_timeout_ms") @Nullable @Min(1) Long sessionTimeoutMs,
                                           @JsonProperty("roles") @Nullable List<String> roles) {
        return new AutoValue_ChangeUserRequest(email, firstName, lastName, permissions, timezone, startpage, sessionTimeoutMs, roles);
    }
}
