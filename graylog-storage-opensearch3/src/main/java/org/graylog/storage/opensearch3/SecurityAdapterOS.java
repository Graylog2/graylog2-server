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
package org.graylog.storage.opensearch3;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog2.indexer.security.SecurityAdapter;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.security.GetRoleMappingResponse;
import org.opensearch.client.opensearch.security.PatchRoleMappingResponse;
import org.opensearch.client.opensearch.security.RoleMapping;

import java.io.IOException;
import java.util.List;

/**
 * see: https://opensearch.org/docs/latest/security/access-control/api/
 */
public class SecurityAdapterOS implements SecurityAdapter {

    private final OfficialOpensearchClient client;

    @Inject
    public SecurityAdapterOS(OfficialOpensearchClient openSearchClient) {
        this.client = openSearchClient;
    }

    @Override
    public Mapping getMappingForRole(final String role) {
        try {
            final GetRoleMappingResponse response = client.sync().security().getRoleMapping(r -> r.role(role));
            final RoleMapping roleMapping = response.get(role);
            return new Mapping(roleMapping.backendRoles(), roleMapping.hosts(), roleMapping.users());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MappingResponse addUserToRoleMapping(final String role, final String user) {
        if (getMappingForRole(role).users().contains(user)) {
            return MappingResponse.OK_USER_ALREADY_IN_MAPPING;
        } else {
            return patchRoleUsers(role, "add", user);
        }
    }

    public MappingResponse removeUserFromRoleMapping(final String role, final String user) {
        return patchRoleUsers(role, "remove", user);
    }

    @Nonnull
    private MappingResponse patchRoleUsers(String role, String operation, String user) {
        try {
            final PatchRoleMappingResponse response = client.sync().security().patchRoleMapping(r -> r.role(role).operations(op -> op.path("/users").op(operation).value(JsonData.of(List.of(user)))));
            return new MappingResponse(response.status(), response.message());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
