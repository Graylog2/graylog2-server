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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class RoleResponse {

    @JsonProperty
    @NotBlank
    public abstract String name();

    @JsonProperty
    public abstract Optional<String> description();

    @JsonProperty
    @NotNull
    public abstract Set<String> permissions();

    @JsonProperty
    public abstract boolean readOnly();

    @JsonCreator
    public static RoleResponse create(@JsonProperty("name") @NotBlank String name,
                                      @JsonProperty("description") Optional<String> description,
                                      @JsonProperty("permissions") @NotNull Set<String> permissions,
                                      @JsonProperty("read_only") boolean readOnly) {
        return new AutoValue_RoleResponse(name, description, permissions, readOnly);
    }
}
