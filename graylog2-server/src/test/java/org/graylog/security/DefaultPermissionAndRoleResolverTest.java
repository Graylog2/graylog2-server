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
package org.graylog.security;

import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog.security.permissions.CaseSensitiveWildcardPermission;
import org.graylog.security.permissions.GRNPermission;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(GRNExtension.class)
class DefaultPermissionAndRoleResolverTest {
    private DefaultPermissionAndRoleResolver resolver;

    @BeforeEach
    void setUp(MongoCollections mongoCollections, GRNRegistry grnRegistry) {
        final var dbService = new DBGrantService(mongoCollections);
        final var capabilities = new CapabilityRegistry(grnRegistry, Set.of(
                new RestPermissions(),
                new ViewsRestPermissions()
        ));

        this.resolver = new DefaultPermissionAndRoleResolver(capabilities, dbService);
    }

    @Test
    @MongoDBFixtures("grants.json")
    void resolvePermissionsForPrincipalJane() {
        final var permissions = resolver.resolvePermissionsForPrincipal(GRNTypes.USER.toGRN("jane"));

        assertThat(permissions).containsExactlyInAnyOrder(
                GRNPermission.create("entity:own", GRNTypes.DASHBOARD.toGRN("54e3deadbeefdeadbeef0000")),
                new CaseSensitiveWildcardPermission("dashboards:edit:54e3deadbeefdeadbeef0000"),
                new CaseSensitiveWildcardPermission("dashboards:read:54e3deadbeefdeadbeef0000"),
                new CaseSensitiveWildcardPermission("streams:changestate:54e3deadbeefdeadbeef0001"),
                new CaseSensitiveWildcardPermission("streams:edit:54e3deadbeefdeadbeef0001"),
                new CaseSensitiveWildcardPermission("streams:read:54e3deadbeefdeadbeef0000"),
                new CaseSensitiveWildcardPermission("streams:read:54e3deadbeefdeadbeef0001"),
                new CaseSensitiveWildcardPermission("view:delete:54e3deadbeefdeadbeef0000"),
                new CaseSensitiveWildcardPermission("view:edit:54e3deadbeefdeadbeef0000"),
                new CaseSensitiveWildcardPermission("view:read:54e3deadbeefdeadbeef0000")
        );
    }

    @Test
    @MongoDBFixtures("grants.json")
    void resolvePermissionsForPrincipalJohn() {
        final var permissions = resolver.resolvePermissionsForPrincipal(GRNTypes.USER.toGRN("john"));

        assertThat(permissions).containsExactlyInAnyOrder(
                GRNPermission.create("entity:own", GRNTypes.DASHBOARD.toGRN("54e3deadbeefdeadbeef0001")),
                new CaseSensitiveWildcardPermission("dashboards:edit:54e3deadbeefdeadbeef0001"),
                new CaseSensitiveWildcardPermission("dashboards:read:54e3deadbeefdeadbeef0001"),
                new CaseSensitiveWildcardPermission("streams:read:54e3deadbeefdeadbeef0001"),
                new CaseSensitiveWildcardPermission("view:delete:54e3deadbeefdeadbeef0001"),
                new CaseSensitiveWildcardPermission("view:edit:54e3deadbeefdeadbeef0001"),
                new CaseSensitiveWildcardPermission("view:read:54e3deadbeefdeadbeef0001")
        );
    }

    @Test
    @MongoDBFixtures("grants.json")
    void resolvePermissionsForPrincipalWithWrongPrincipalType() {
        final var permissions = resolver.resolvePermissionsForPrincipal(GRNTypes.DASHBOARD.toGRN("jane"));

        // The stream is shared with everyone, so we expect the read permission for the stream even though the
        // principal is not a user. We might want to change this in the future, but for now this is the expected behavior.
        assertThat(permissions)
                .containsExactly(new CaseSensitiveWildcardPermission("streams:read:54e3deadbeefdeadbeef0001"));
    }
}
