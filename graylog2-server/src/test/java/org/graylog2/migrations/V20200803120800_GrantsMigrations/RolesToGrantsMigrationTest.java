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
package org.graylog2.migrations.V20200803120800_GrantsMigrations;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.TestUserService;
import org.graylog.testing.TestUserServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(TestUserServiceExtension.class)
@MongoDBFixtures("V20200803120800_MigrateRolesToGrantsTest.json")
class RolesToGrantsMigrationTest {
    private RolesToGrantsMigration migration;
    private RoleService roleService;
    private DBGrantService dbGrantService;

    @Mock
    private Permissions permissions;

    @Mock
    private Validator validator;
    private GRNRegistry grnRegistry;

    private UserService userService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
               GRNRegistry grnRegistry,
               TestUserService userService) {
        when(permissions.readerBasePermissions()).thenReturn(ImmutableSet.of());
        when(validator.validate(any())).thenReturn(ImmutableSet.of());

        this.grnRegistry = grnRegistry;

        roleService = new RoleServiceImpl(mongodb.mongoConnection(), mongoJackObjectMapperProvider, permissions, validator);

        dbGrantService = new DBGrantService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, grnRegistry);
        this.userService = userService;
        DBGrantService dbGrantService = new DBGrantService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, grnRegistry);
        migration = new RolesToGrantsMigration(roleService, userService, dbGrantService, grnRegistry, "admin");
    }

    @Test
    void migrateSimpleRole() throws NotFoundException {

        final User testuser1 = userService.load("testuser1");
        assertThat(testuser1).isNotNull();
        final User testuser2 = userService.load("testuser2");
        assertThat(testuser2).isNotNull();

        assertThat(roleService.load("mig-test")).isNotNull();
        assertThat(dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser1)))).isEmpty();

        migration.upgrade();

        // check created grants for testuser1
        final ImmutableSet<GrantDTO> grants = dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser1)));
        assertGrantInSet(grants, "grn::::dashboard:5e2afc66cd19517ec2dabadd", Capability.VIEW);
        assertGrantInSet(grants, "grn::::dashboard:5e2afc66cd19517ec2dabadf", Capability.MANAGE);
        assertGrantInSet(grants, "grn::::stream:5c40ad603c034441a56942bd", Capability.VIEW);
        assertGrantInSet(grants, "grn::::stream:5e2f5cfb4868e67ad4da562d", Capability.VIEW);
        assertGrantInSet(grants, "grn::::stream:000000000000000000000002", Capability.MANAGE);
        assertThat(grants.size()).isEqualTo(5);

        // testuser2 gets the same grants. a simple check should suffice
        assertThat(dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser2)))).satisfies(grantDTOS -> {
            assertThat(Iterables.size(grantDTOS)).isEqualTo(5);
        });

        // empty role should be dropped
        assertThatThrownBy(() -> roleService.load("mig-test")).isInstanceOf(NotFoundException.class);

    }

    @Test
    public void nonMigratableRole() throws NotFoundException {

        final User testuser3 = userService.load("testuser3");
        assertThat(testuser3).isNotNull();

        assertThat(roleService.load("non-migratable-role")).satisfies(role -> {
            assertThat(role.getPermissions().size()).isEqualTo(4);
        });
        assertThat(dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser3)))).isEmpty();
        migration.upgrade();

        assertThat(roleService.load("non-migratable-role")).satisfies(role -> {
            assertThat(role.getPermissions().size()).isEqualTo(4);
        });
        assertThat(dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser3)))).isEmpty();

    }

    @Test
    public void partlyMigratableRole() throws NotFoundException {

        final User testuser4 = userService.load("testuser3");
        assertThat(testuser4).isNotNull();

        assertThat(roleService.load("partly-migratable-role")).satisfies(role -> {
            assertThat(role.getPermissions().size()).isEqualTo(5);
        });
        assertThat(dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser4)))).isEmpty();
        migration.upgrade();

        assertThat(roleService.load("partly-migratable-role")).satisfies(role -> {
            assertThat(role.getPermissions().size()).isEqualTo(3);
        });
        assertThat(dbGrantService.getForGranteesOrGlobal(ImmutableSet.of(grnRegistry.ofUser(testuser4)))).isEmpty();

    }

    private void assertGrantInSet(Set<GrantDTO> grants, String target, Capability capability) {
        assertThat(grants.stream().anyMatch(g -> g.target().equals(grnRegistry.parse(target)) && g.capability().equals(capability))).isTrue();
    }

}
