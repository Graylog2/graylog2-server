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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(TestUserServiceExtension.class)
@ExtendWith(MockitoExtension.class)
public class RecentActivityServiceTest {
    // to test the capping of the collection, we want a reasonaböy small maximum
    final static int MAXIMUM = 10;

    RecentActivityService recentActivityService;
    TestUserService testUserService;

    @Mock
    PermissionAndRoleResolver permissionAndRoleResolver;

    User user;
    SearchUser searchUser;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
               GRNRegistry grnRegistry,
               TestUserService testUserService) {

        this.testUserService = testUserService;
        this.recentActivityService = new RecentActivityService(mongodb.mongoConnection(),
                mongoJackObjectMapperProvider,
               null,
                grnRegistry,
                permissionAndRoleResolver,
                MAXIMUM);

        user = TestUser.builder().withId("1").withUsername("test").isLocalAdmin(true).build();
        searchUser = TestSearchUser.builder().withUser(user).build();
        final var principal = grnRegistry.newGRN(GRNTypes.USER, user.getId());
//        when(permissionAndRoleResolver.resolveGrantees(any())).thenReturn(Set.of(principal));
    }

    @Test
    public void testCappedCollection() {
        var activities = recentActivityService.findRecentActivitiesFor(searchUser, 1, MAXIMUM + 1);
        assertThat(activities.pagination().total()).isEqualTo(0);
        assertThat(activities.grandTotal().isEmpty()).isFalse();
        assertThat(activities.grandTotal().get()).isEqualTo(0);

        for(int i = 0; i <= MAXIMUM; i++) {
            var activity = RecentActivityDTO.builder()
                    .activityType(ActivityType.CREATE)
                    .itemGrn("grn:" + i)
                    .grantee(searchUser.getUser().getId())
                    .itemId(""+ i)
                    .userName(searchUser.username())
                    .itemTitle("" + i)
                    .itemType("TEST")
                    .build();
            recentActivityService.save(activity);
        }

        activities = recentActivityService.findRecentActivitiesFor(searchUser, 1, MAXIMUM + 1);
        assertThat(activities.pagination().total()).isEqualTo(MAXIMUM);
        assertThat(activities.grandTotal().isEmpty()).isFalse();
        assertThat(activities.grandTotal().get()).isEqualTo(MAXIMUM);

        // check that the first inserted element has been removed because of capping
        assertThat(activities.delegate().stream().filter(activity -> Objects.equals(activity.itemId(), "0")).toList().isEmpty()).isTrue();
    }

    @Test
    public void testFilteringForGrantees() {

    }
}
