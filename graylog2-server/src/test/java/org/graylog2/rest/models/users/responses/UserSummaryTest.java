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
package org.graylog2.rest.models.users.responses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSummaryTest {

    private GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();
    private ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private final UserSummary userSummary = UserSummary.create(
            "1234",
            "user",
            "email",
            "Hans",
            "Dampf",
            "Hans Dampf",
            ImmutableList.of(new WildcardPermission("dashboard:create:123")),
            ImmutableList.of(GRNPermission.create(RestPermissions.ENTITY_OWN, grnRegistry.newGRN(GRNTypes.STREAM, "1234"))),
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            true,
            null,
            null,
            User.AccountStatus.ENABLED
    );

    @Test
    void permissionsSerialization() {
        final JsonNode jsonNode = objectMapper.convertValue(userSummary, JsonNode.class);
        assertThat(jsonNode.isObject()).isTrue();
        assertThat(jsonNode.path("permissions").get(0).asText()).isEqualTo("dashboard:create:123");
    }

    @Test
    void grnPermissionsSerialization() {
        final JsonNode jsonNode = objectMapper.convertValue(userSummary, JsonNode.class);
        assertThat(jsonNode.isObject()).isTrue();
        assertThat(jsonNode.path("grn_permissions").get(0).asText()).isEqualTo("entity:own:grn::::stream:1234");
    }
}
