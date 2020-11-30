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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleImpl;
import org.graylog2.users.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("view-sharings.json")
class ViewSharingToGrantsMigrationTest {
    private ViewSharingToGrantsMigration migration;
    private DBGrantService grantService;
    private UserService userService;
    private RoleService roleService;
    private MongoCollection<Document> dbCollection;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider objectMapperProvider,
               GRNRegistry grnRegistry,
               @Mock ClusterConfigService clusterConfigService,
               @Mock UserService userService,
               @Mock RoleService roleService) {

        this.dbCollection = mongodb.mongoCollection("view_sharings");
        this.userService = userService;
        this.roleService = roleService;
        this.grantService = new DBGrantService(mongodb.mongoConnection(), objectMapperProvider, grnRegistry);

        when(userService.load(anyString())).thenAnswer(a -> {
            final String argument = a.getArgument(0);
            return createUser(argument);
        });

        final EntityOwnershipService entityOwnershipService = new EntityOwnershipService(grantService, grnRegistry);
        final TestViewService viewService = new TestViewService(mongodb.mongoConnection(), objectMapperProvider, clusterConfigService, entityOwnershipService);

        this.migration = new ViewSharingToGrantsMigration(mongodb.mongoConnection(), grantService, userService, roleService, "admin", viewService, grnRegistry);
    }

    private void assertDeletedViewSharing(String id) {
        assertThat(dbCollection.countDocuments(Filters.eq("_id", new ObjectId(id))))
                .isEqualTo(0);
    }

    @Test
    @DisplayName("migrate user shares")
    void migrateUserShares() throws Exception {
        final GRN jane = GRNTypes.USER.toGRN("jane");
        final GRN john = GRNTypes.USER.toGRN("john");
        final GRN search = GRNTypes.SEARCH.toGRN("54e3deadbeefdeadbeef0001");

        when(roleService.load(anyString())).thenThrow(new NotFoundException());

        assertThat(grantService.hasGrantFor(jane, Capability.VIEW, search)).isFalse();
        assertThat(grantService.hasGrantFor(john, Capability.VIEW, search)).isFalse();

        migration.upgrade();

        assertThat(grantService.hasGrantFor(jane, Capability.VIEW, search)).isTrue();
        assertThat(grantService.hasGrantFor(john, Capability.VIEW, search)).isTrue();

        assertThat(grantService.hasGrantFor(jane, Capability.OWN, search)).isFalse();
        assertThat(grantService.hasGrantFor(jane, Capability.MANAGE, search)).isFalse();
        assertThat(grantService.hasGrantFor(john, Capability.OWN, search)).isFalse();
        assertThat(grantService.hasGrantFor(john, Capability.MANAGE, search)).isFalse();

        assertDeletedViewSharing("54e3deadbeefdeadbeef0001");
    }

    @Test
    @DisplayName("migrate role shares")
    void migrateRoleShares() throws Exception {
        final User userJane = createUser("jane");
        final User userJohn = createUser("john");
        final Role role1 = createRole("role1");
        final Role role2 = createRole("role2");

        when(userService.loadAllForRole(role1)).thenReturn(ImmutableSet.of(userJane, userJohn));
        when(userService.loadAllForRole(role2)).thenReturn(Collections.emptySet());
        when(roleService.load(role1.getName())).thenReturn(role1);
        when(roleService.load(role2.getName())).thenReturn(role2);

        final GRN jane = GRNTypes.USER.toGRN(userJane.getName());
        final GRN john = GRNTypes.USER.toGRN(userJohn.getName());
        final GRN dashboard1 = GRNTypes.DASHBOARD.toGRN("54e3deadbeefdeadbeef0002");

        assertThat(grantService.hasGrantFor(jane, Capability.VIEW, dashboard1)).isFalse();
        assertThat(grantService.hasGrantFor(john, Capability.VIEW, dashboard1)).isFalse();

        migration.upgrade();

        assertThat(grantService.hasGrantFor(jane, Capability.VIEW, dashboard1)).isTrue();
        assertThat(grantService.hasGrantFor(john, Capability.VIEW, dashboard1)).isTrue();

        assertThat(grantService.hasGrantFor(jane, Capability.OWN, dashboard1)).isFalse();
        assertThat(grantService.hasGrantFor(jane, Capability.MANAGE, dashboard1)).isFalse();
        assertThat(grantService.hasGrantFor(john, Capability.OWN, dashboard1)).isFalse();
        assertThat(grantService.hasGrantFor(john, Capability.MANAGE, dashboard1)).isFalse();

        assertDeletedViewSharing("54e3deadbeefdeadbeef0002");
    }

    @Test
    @DisplayName("migrate all-of-instance shares")
    void migrateAllOfInstanceShares() throws Exception {

        final GRN everyone = GRNRegistry.GLOBAL_USER_GRN;
        when(roleService.load(anyString())).thenThrow(new NotFoundException());

        final GRN dashboard2 = GRNTypes.DASHBOARD.toGRN("54e3deadbeefdeadbeef0003");

        assertThat(grantService.hasGrantFor(everyone, Capability.VIEW, dashboard2)).isFalse();

        migration.upgrade();

        assertThat(grantService.hasGrantFor(everyone, Capability.VIEW, dashboard2)).isTrue();

        assertThat(grantService.hasGrantFor(everyone, Capability.OWN, dashboard2)).isFalse();
        assertThat(grantService.hasGrantFor(everyone, Capability.MANAGE, dashboard2)).isFalse();

        assertDeletedViewSharing("54e3deadbeefdeadbeef0003");
    }

    private User createUser(String name) {
        final User user = mock(User.class);
        lenient().when(user.getName()).thenReturn(name);
        lenient().when(user.getId()).thenReturn(name);
        return user;
    }

    private Role createRole(String name) {
        final RoleImpl role = new RoleImpl();

        role._id = new ObjectId().toHexString();
        role.setName(name);
        role.setNameLower(name.toLowerCase(Locale.US));
        role.setDescription("This is role: " + name);

        return role;
    }

    public static class TestViewService extends ViewService {
        public TestViewService(MongoConnection mongoConnection,
                               MongoJackObjectMapperProvider mapper,
                               ClusterConfigService clusterConfigService,
                               EntityOwnershipService entityOwnerShipService) {
            super(mongoConnection, mapper, clusterConfigService, view -> new ViewRequirements(Collections.emptySet(), view), entityOwnerShipService);
        }
    }
}
