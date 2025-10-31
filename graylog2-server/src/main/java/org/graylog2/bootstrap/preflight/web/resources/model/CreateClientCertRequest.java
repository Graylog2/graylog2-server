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
package org.graylog2.bootstrap.preflight.web.resources.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @param role deprecated, use list of roles instead
 */
public record CreateClientCertRequest(
        @JsonProperty("principal") String principal,
        @JsonProperty("role") @Deprecated String role,
        @JsonProperty("roles") @Nullable List<String> roles,
        @JsonProperty("password") String password,
        @JsonProperty("certificate_lifetime") @NotNull String certificateLifetime
) {
    @Override
    public List<String> roles() {
        if (roles != null && !roles.isEmpty()) {
            return roles;
        } else {
            return List.of(role);
        }
    }
}
