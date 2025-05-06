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
package org.graylog2.security;

import com.google.common.eventbus.EventBus;
import jakarta.annotation.Nonnull;
import jakarta.validation.Validator;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.assertj.core.api.Assertions;
import org.graylog.security.permissions.CaseSensitiveWildcardPermission;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.users.RoleImpl;
import org.graylog2.users.RoleService;
import org.graylog2.users.RoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Set;

@ExtendWith(MongoDBExtension.class)
class InMemoryRolePermissionResolverTest {
    @Test
    void testRoleChangedEventHandling(MongoCollections mongoCollections) throws NotFoundException, ValidationException {
        final EventBus eventBus = new EventBus();
        final ClusterEventBus clusterEventBus = new ClusterEventBus() {
            @Override
            public void post(@Nonnull Object event) {
                eventBus.post(event);
            }
        };
        final RoleService service = new RoleServiceImpl(mongoCollections, new Permissions(Collections.emptySet()), Mockito.mock(Validator.class), clusterEventBus);
        final RolePermissionResolver resolver = new InMemoryRolePermissionResolver(service, eventBus);

        // test that the built-in roles are present right after resolver init
        Assertions.assertThat(resolver.resolvePermissionsInRole(service.getAdminRoleObjectId()))
                .anySatisfy(permission -> permission.implies(new AllPermission()));

        // now let the service create a role. This information should be immediately propagated to the resolver
        final Role createdRole = service.save(createRole("inputs_manager", "manages inputs", Set.of(RestPermissions.INPUTS_READ, RestPermissions.INPUTS_CREATE)));

        // without explicitly updating the resolver (relying on the event bus), let's check that the role and its permissions
        // are available.
        Assertions.assertThat(resolver.resolvePermissionsInRole(createdRole.getId()))
                .hasSize(2)
                .anySatisfy(permission -> permission.implies(new CaseSensitiveWildcardPermission(RestPermissions.INPUTS_READ)))
                .anySatisfy(permission -> permission.implies(new CaseSensitiveWildcardPermission(RestPermissions.INPUTS_CREATE)));

        // now let's delete a role and check that the resolver got rid of it as well
        service.delete("inputs_manager");
        Assertions.assertThat(resolver.resolvePermissionsInRole(createdRole.getId()))
                .isEmpty();
    }


    @Nonnull
    private static RoleImpl createRole(String name, String description, Set<String> permissions) {
        final RoleImpl role = new RoleImpl();
        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions);
        role.setReadOnly(false);
        return role;
    }
}
