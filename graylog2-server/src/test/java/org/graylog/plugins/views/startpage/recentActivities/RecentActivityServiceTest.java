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
package org.graylog.plugins.views.startpage.recentActivities;

import org.apache.shiro.authz.Permission;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.rest.TestUser;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.TestUserService;
import org.graylog.testing.TestUserServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(TestUserServiceExtension.class)
public class RecentActivityServiceTest {
    // to test the capping of the collection, we want a reasonaböy small maximum
    final static int MAXIMUM = 10;

    RecentActivityService recentActivityService;
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
        this.recentActivityService = new RecentActivityService(mongodb.mongoConnection(),
                mongoJackObjectMapperProvider,
               null,
                grnRegistry,
                permissionAndRoleResolver,
                MAXIMUM);
     }

    @Test
    public void testCappedCollection() {
        var activities = recentActivityService.findRecentActivitiesFor(searchAdmin, 1, MAXIMUM + 1);
        assertThat(activities.pagination().total()).isEqualTo(0);
        assertThat(activities.grandTotal().isEmpty()).isFalse();
        assertThat(activities.grandTotal().get()).isEqualTo(0);

        for(int i = 0; i <= MAXIMUM; i++) {
            var activity = RecentActivityDTO.builder()
                    .activityType(ActivityType.CREATE)
                    .itemGrn(grnRegistry.newGRN(GRNTypes.DASHBOARD, "" + i))
                    .grantee("invalid")
                    .userName(searchUser.username())
                    .itemTitle("" + i)
                    .build();
            recentActivityService.save(activity);
        }

        activities = recentActivityService.findRecentActivitiesFor(searchAdmin, 1, MAXIMUM + 1);
        assertThat(activities.pagination().total()).isEqualTo(MAXIMUM);
        assertThat(activities.grandTotal().isEmpty()).isFalse();
        assertThat(activities.grandTotal().get()).isEqualTo(MAXIMUM);

        // check that the first inserted element has been removed because of capping
        assertThat(activities.delegate().stream().filter(activity -> Objects.equals(activity.itemGrn().entity(), "0")).toList().isEmpty()).isTrue();
    }

    @Test
    public void testFilteringForGrantees() {
        var activity = RecentActivityDTO.builder()
                .activityType(ActivityType.CREATE)
                .itemGrn(grnRegistry.newGRN(GRNTypes.DASHBOARD, "testforuser"))
                .grantee(grnRegistry.newGRN(GRNTypes.USER, user.getId()).toString())
                .userName(searchAdmin.username())
                .itemTitle("1")
                .build();
        recentActivityService.save(activity);
        var activities = recentActivityService.findRecentActivitiesFor(searchUser, 1, MAXIMUM + 1);
        assertThat(activities.pagination().total()).isEqualTo(1);
        assertThat(activities.grandTotal().isEmpty()).isFalse();
        assertThat(activities.grandTotal().get()).isEqualTo(1);

        assertThat(activities.delegate().stream().filter(a -> Objects.equals(a.itemGrn().entity(), "testforuser")).toList().size()).isEqualTo(1);
    }

    private void createActivity(final String id, final ActivityType activityType) {
        recentActivityService.save(
                RecentActivityDTO.builder()
                        .activityType(activityType)
                        .itemGrn(grnRegistry.newGRN(GRNTypes.SEARCH_FILTER, id))
                        .grantee(grnRegistry.newGRN(GRNTypes.USER, user.getId()).toString())
                        .userName(searchAdmin.username())
                        .itemTitle(GRNTypes.SEARCH_FILTER.type() + " with id " + id)
                        .build()
        );
    }

    @Test
    public void testEntityWasDeleted() {
        createActivity("1", ActivityType.CREATE);
        createActivity("2", ActivityType.CREATE);
        createActivity("3", ActivityType.CREATE);
        createActivity("1", ActivityType.UPDATE);

        var activities = recentActivityService.findRecentActivitiesFor(searchUser, 1, MAXIMUM + 1);
        assertThat(activities.pagination().total()).isEqualTo(4);
        assertThat(activities.grandTotal().isEmpty()).isFalse();
        assertThat(activities.grandTotal().get()).isEqualTo(4);

        recentActivityService.deleteAllEntriesForEntity(grnRegistry.newGRN(GRNTypes.SEARCH_FILTER, "1"));

        activities = recentActivityService.findRecentActivitiesFor(searchUser, 1, MAXIMUM + 1);
        assertThat(activities.pagination().total()).isEqualTo(2);
        assertThat(activities.grandTotal().isEmpty()).isFalse();
        assertThat(activities.grandTotal().get()).isEqualTo(2);

        assertThat(activities.delegate().stream().filter(a -> Objects.equals(a.itemGrn().entity(), "2")).toList().size()).isEqualTo(1);
        assertThat(activities.delegate().stream().filter(a -> Objects.equals(a.itemGrn().entity(), "3")).toList().size()).isEqualTo(1);
    }
}
