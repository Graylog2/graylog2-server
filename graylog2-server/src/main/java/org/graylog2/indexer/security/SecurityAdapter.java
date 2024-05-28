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
package org.graylog2.indexer.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public interface SecurityAdapter {
    record MappingResponse(String status, String message) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Mapping(@JsonProperty List<String> backendRoles, @JsonProperty List<String> hosts, @JsonProperty List<String> users) {}

    Mapping getMappingForRole(final String role);
    MappingResponse addUserToRoleMapping(final String role, final String user);
    MappingResponse removeUserFromRoleMapping(final String role, final String user);

}
