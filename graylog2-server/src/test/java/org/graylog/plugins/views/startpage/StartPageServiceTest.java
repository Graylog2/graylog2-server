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
package org.graylog.plugins.views.startpage;

import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.Permission;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.rest.TestUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpened;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedForUserDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedService;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog.security.DBGrantService;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.TestUserService;
import org.graylog.testing.TestUserServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.lookup.Catalog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(TestUserServiceExtension.class)
public class StartPageServiceTest {
    private static final long MAX = 10;

    private StartPageService startPageService;

    private SearchUser searchUser;

    static class TestCatalog extends Catalog {
        public TestCatalog() {
            super(null);
        }

        @Override
        protected LoadingCache<String, Entry> createCache() {
            return null;
        }

        @Override
        public String getTitle(final String id) {
            return "";
        }

        @Override
        public String getType(final String id) {
            return "";
        }
    }

    @BeforeEach
    public void init(MongoDBTestService mongodb,
                     MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
                     GRNRegistry grnRegistry,
                     TestUserService testUserService) {
        var admin = TestUser.builder().withId("637748db06e1d74da0a54331").withUsername("local:admin").isLocalAdmin(true).build();
        var user = TestUser.builder().withId("637748db06e1d74da0a54330").withUsername("test").isLocalAdmin(false).build();
        searchUser = TestSearchUser.builder().withUser(user).build();
        var searchAdmin = TestSearchUser.builder().withUser(admin).build();

        var permissionAndRoleResolver = new PermissionAndRoleResolver() {
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

        var eventbus = new EventBus();
        var dbGrantService = new DBGrantService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, grnRegistry);
        var entityOwnerShipService = new EntityOwnershipService(dbGrantService, grnRegistry);
        var lastOpenedService = new LastOpenedService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, eventbus, entityOwnerShipService);
        var recentActivityService = new RecentActivityService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, eventbus, grnRegistry, permissionAndRoleResolver);
        startPageService = new StartPageService(new TestCatalog(), lastOpenedService, recentActivityService, eventbus);
    }

    @Test
    public void testCreateLastOpenedForUser() {
        startPageService.addLastOpenedFor(ViewDTO.builder().id("id1").title("test").state(new HashMap<>()).searchId("1").build(), searchUser);
        var result = startPageService.findLastOpenedFor(searchUser, 1, 10);
        var list = (List<LastOpened>)result.jsonValue().get("lastOpened");
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).id()).isEqualTo("id1");

        startPageService.addLastOpenedFor(ViewDTO.builder().id("id2").title("test").state(new HashMap<>()).searchId("1").build(), searchUser);
        result = startPageService.findLastOpenedFor(searchUser, 1, 10);
        list = (List<LastOpened>)result.jsonValue().get("lastOpened");
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).id()).isEqualTo("id2");
        assertThat(list.get(1).id()).isEqualTo("id1");

        startPageService.addLastOpenedFor(ViewDTO.builder().id("id3").title("test").state(new HashMap<>()).searchId("1").build(), searchUser);
        startPageService.addLastOpenedFor(ViewDTO.builder().id("id1").title("test").state(new HashMap<>()).searchId("1").build(), searchUser);
        result = startPageService.findLastOpenedFor(searchUser, 1, 10);
        list = (List<LastOpened>)result.jsonValue().get("lastOpened");
        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0).id()).isEqualTo("id1");
        assertThat(list.get(1).id()).isEqualTo("id3");
    }

    @Test
    public void testRemoveIfExistsInList() {
        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", List.of(new LastOpenedDTO("1", DateTime.now(DateTimeZone.UTC))));

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, "1", MAX);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void testDontRemoveIfExistsInList() {
        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", List.of(new LastOpenedDTO("1", DateTime.now(DateTimeZone.UTC))));

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, "2", MAX);
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    public void testCapAtMaximumMinusOneSoYouCanAddANewElement() {
        var list = List.of(
                new LastOpenedDTO("1", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("2", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("3", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("4", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("5", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("6", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("7", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("8", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("9", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("10", DateTime.now(DateTimeZone.UTC))
        );
        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", list);

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, "11", MAX);
        assertThat(result.size()).isEqualTo(9);
    }

    @Test
    public void testNotCapAtMaximumMinusOneIfYouAreAtMaxMinusOne() {
        var list = List.of(
                new LastOpenedDTO("1", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("2", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("3", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("4", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("5", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("6", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("7", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("8", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("9", DateTime.now(DateTimeZone.UTC))
        );
        assertThat(list.size()).isEqualTo(MAX-1);
        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", list);

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, "11", MAX);
        assertThat(result.size()).isEqualTo(MAX-1);
    }

    @Test
    public void testRemoveItemFromTheMiddle() {
        var list = List.of(
                new LastOpenedDTO("1", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("2", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("3", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("4", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("5", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("6", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("7", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("8", DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO("9", DateTime.now(DateTimeZone.UTC))
        );
        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", list);

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, "5", MAX);
        assertThat(result.size()).isEqualTo(8);
    }
}
