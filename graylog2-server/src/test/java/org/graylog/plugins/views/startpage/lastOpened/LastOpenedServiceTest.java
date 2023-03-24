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
package org.graylog.plugins.views.startpage.lastOpened;

import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.Permission;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.rest.TestUser;
import org.graylog.plugins.views.startpage.recentActivities.ActivityType;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityEvent;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.TestUserService;
import org.graylog.testing.TestUserServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.database.users.User;
import org.graylog2.users.events.UserDeletedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(TestUserServiceExtension.class)
public class LastOpenedServiceTest {
    LastOpenedService lastOpenedService;
    TestUserService testUserService;
    GRNRegistry grnRegistry;

    PermissionAndRoleResolver permissionAndRoleResolver;

    User user;
    User admin;
    SearchUser searchUser;
    SearchUser searchAdmin;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
               GRNRegistry grnRegistry,
               TestUserService testUserService) {

        admin = TestUser.builder().withId("637748db06e1d74da0a54331").withUsername("local:admin").isLocalAdmin(true).build();
        user = TestUser.builder().withId("637748db06e1d74da0a54330").withUsername("test").isLocalAdmin(false).build();
        searchUser = TestSearchUser.builder().withUser(user).build();
        searchAdmin = TestSearchUser.builder().withUser(admin).build();

        permissionAndRoleResolver = new PermissionAndRoleResolver() {
            @Override
            public Set<Permission> resolvePermissionsForPrincipal(GRN principal) {
                return null;
            }

            @Override
            public Set<String> resolveRolesForPrincipal(GRN principal) {
                return null;
            }

            @Override
            public Set<GRN> resolveGrantees(GRN principal) {
                return Set.of(grnRegistry.newGRN(GRNTypes.USER, user.getId()));
            }
        };

        this.testUserService = testUserService;
        this.grnRegistry = grnRegistry;
        this.lastOpenedService = new LastOpenedService(mongodb.mongoConnection(),
                mongoJackObjectMapperProvider,
                new EventBus(),
                null);
    }

    @Test
    public void testRemoveOnUserDeletion() {
        var _1 = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");
        var _2 = grnRegistry.newGRN(GRNTypes.SEARCH, "2");

        lastOpenedService.save(new LastOpenedForUserDTO("user1", List.of(new LastOpenedDTO(_1 ,DateTime.now(DateTimeZone.UTC)), new LastOpenedDTO(_2 ,DateTime.now(DateTimeZone.UTC)))));
        lastOpenedService.save(new LastOpenedForUserDTO("user2", List.of(new LastOpenedDTO(_1 ,DateTime.now(DateTimeZone.UTC)), new LastOpenedDTO(_2 ,DateTime.now(DateTimeZone.UTC)))));
        lastOpenedService.save(new LastOpenedForUserDTO("user3", List.of(new LastOpenedDTO(_1 ,DateTime.now(DateTimeZone.UTC)), new LastOpenedDTO(_2 ,DateTime.now(DateTimeZone.UTC)))));

        assertThat(lastOpenedService.streamAll().toList().size()).isEqualTo(3);
        lastOpenedService.removeFavoriteEntityOnUserDeletion(UserDeletedEvent.create("user2", "user2"));
        assertThat(lastOpenedService.streamAll().toList().size()).isEqualTo(2);
        assertThat(lastOpenedService.findForUser("user1")).isNotEmpty();
        assertThat(lastOpenedService.findForUser("user3")).isNotEmpty();
    }

    @Test
    public void testRemoveOnEntityDeletion() {
        var _1 = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");
        var _2 = grnRegistry.newGRN(GRNTypes.SEARCH, "2");
        var _3 = grnRegistry.newGRN(GRNTypes.SEARCH, "3");
        var _4 = grnRegistry.newGRN(GRNTypes.DASHBOARD, "4");

        lastOpenedService.save(new LastOpenedForUserDTO("user1", List.of(new LastOpenedDTO(_2 ,DateTime.now(DateTimeZone.UTC)))));
        lastOpenedService.save(new LastOpenedForUserDTO("user2", List.of(new LastOpenedDTO(_1 ,DateTime.now(DateTimeZone.UTC)), new LastOpenedDTO(_2 , DateTime.now(DateTimeZone.UTC)))));
        lastOpenedService.save(new LastOpenedForUserDTO("user3", List.of(new LastOpenedDTO(_1 ,DateTime.now(DateTimeZone.UTC)), new LastOpenedDTO(_2 ,DateTime.now(DateTimeZone.UTC)), new LastOpenedDTO(_3 ,DateTime.now(DateTimeZone.UTC)))));
        lastOpenedService.save(new LastOpenedForUserDTO("user4", List.of(new LastOpenedDTO(_1 ,DateTime.now(DateTimeZone.UTC)), new LastOpenedDTO(_4 ,DateTime.now(DateTimeZone.UTC)), new LastOpenedDTO(_3 ,DateTime.now(DateTimeZone.UTC)))));

        assertThat(lastOpenedService.streamAll().toList().size()).isEqualTo(4);
        lastOpenedService.removeLastOpenedOnEntityDeletion(new RecentActivityEvent(ActivityType.DELETE, grnRegistry.newGRN(GRNTypes.SEARCH, "2"), "user4"));
        assertThat(lastOpenedService.streamAll().toList().size()).isEqualTo(4);

        var last1 = lastOpenedService.findForUser("user1").get();
        assertThat(last1.items().size()).isEqualTo(0);

        var last2 = lastOpenedService.findForUser("user2").get();
        assertThat(last2.items().size()).isEqualTo(1);
        assertThat(last2.items().get(0).grn().entity()).isEqualTo("1");

        var last3 = lastOpenedService.findForUser("user3").get();
        assertThat(last3.items().size()).isEqualTo(2);
        assertThat(last3.items().get(0).grn().entity()).isEqualTo("1");
        assertThat(last3.items().get(1).grn().entity()).isEqualTo("3");

        var last4 = lastOpenedService.findForUser("user4").get();
        assertThat(last4.items().size()).isEqualTo(3);
    }
}
