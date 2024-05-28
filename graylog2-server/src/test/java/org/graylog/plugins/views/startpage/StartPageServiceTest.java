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
import org.graylog.plugins.views.startpage.title.StartPageItemTitleRetriever;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.TestUserServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.lookup.Catalog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(TestUserServiceExtension.class)
public class StartPageServiceTest {
    private static final long MAX = 10;

    private StartPageService startPageService;

    private SearchUser searchUser;

    private GRNRegistry grnRegistry;

    private Catalog catalog;

    @BeforeEach
    public void init(MongoDBTestService mongodb,
                     MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
                     GRNRegistry grnRegistry) {

        var admin = TestUser.builder().withId("637748db06e1d74da0a54331").withUsername("local:admin").isLocalAdmin(true).build();
        var user = TestUser.builder().withId("637748db06e1d74da0a54330").withUsername("test").isLocalAdmin(false).build();
        this.searchUser = TestSearchUser.builder().withUser(user).build();
        this.grnRegistry = grnRegistry;
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
        final var connection = mongodb.mongoConnection();
        final var collections = new MongoCollections(mongoJackObjectMapperProvider, connection);
        var lastOpenedService = new LastOpenedService(collections, eventbus);
        var recentActivityService = new RecentActivityService(collections, connection, eventbus, grnRegistry, permissionAndRoleResolver);
        catalog = mock(Catalog.class);
        doReturn(Optional.of(new Catalog.Entry("", ""))).when(catalog).getEntry(any());
        startPageService = new StartPageService(grnRegistry, lastOpenedService, recentActivityService, eventbus, new StartPageItemTitleRetriever(catalog, Map.of()));
    }

    @Test
    public void testCreateLastOpenedForUser() {
        final var viewBuilder = ViewDTO.builder().title("test").state(Map.of()).searchId("1");
        startPageService.addLastOpenedFor(viewBuilder.id("id1").build(), searchUser);
        var result = startPageService.findLastOpenedFor(searchUser, 1, 10);
        var list = (List<LastOpened>)result.jsonValue().get("lastOpened");
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).grn().entity()).isEqualTo("id1");

        startPageService.addLastOpenedFor(viewBuilder.id("id2").build(), searchUser);
        result = startPageService.findLastOpenedFor(searchUser, 1, 10);
        list = (List<LastOpened>)result.jsonValue().get("lastOpened");
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).grn().entity()).isEqualTo("id2");
        assertThat(list.get(1).grn().entity()).isEqualTo("id1");

        startPageService.addLastOpenedFor(viewBuilder.id("id3").build(), searchUser);
        startPageService.addLastOpenedFor(viewBuilder.id("id1").build(), searchUser);
        result = startPageService.findLastOpenedFor(searchUser, 1, 10);
        list = (List<LastOpened>)result.jsonValue().get("lastOpened");
        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0).grn().entity()).isEqualTo("id1");
        assertThat(list.get(1).grn().entity()).isEqualTo("id3");
    }

    @Test
    public void testRemoveIfExistsInList() {
        var _1 = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");

        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", List.of(new LastOpenedDTO(_1, DateTime.now(DateTimeZone.UTC))));

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, _1, MAX);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void testDontRemoveIfExistsInList() {
        var _1 = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");
        var _2 = grnRegistry.newGRN(GRNTypes.SEARCH, "2");

        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", List.of(new LastOpenedDTO(_1, DateTime.now(DateTimeZone.UTC))));

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, _2, MAX);
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    public void testCapAtMaximumMinusOneSoYouCanAddANewElement() {
        var list = List.of(
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "2"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "3"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "4"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "5"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "6"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "7"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "8"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "9"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "10"), DateTime.now(DateTimeZone.UTC))
        );
        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", list);

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, grnRegistry.newGRN(GRNTypes.DASHBOARD, "11"), MAX);
        assertThat(result.size()).isEqualTo(9);
    }

    @Test
    public void testNotCapAtMaximumMinusOneIfYouAreAtMaxMinusOne() {
        var list = List.of(
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "2"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "3"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "4"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "5"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "6"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "7"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "8"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "9"), DateTime.now(DateTimeZone.UTC))
        );
        assertThat(list.size()).isEqualTo(MAX-1);
        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", list);

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, grnRegistry.newGRN(GRNTypes.DASHBOARD, "11"), MAX);
        assertThat(result.size()).isEqualTo(MAX-1);
    }

    @Test
    public void testRemoveItemFromTheMiddle() {
        var list = List.of(
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "1"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "2"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "3"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "4"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "5"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "6"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "7"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "8"), DateTime.now(DateTimeZone.UTC)),
                new LastOpenedDTO(grnRegistry.newGRN(GRNTypes.DASHBOARD, "9"), DateTime.now(DateTimeZone.UTC))
        );
        LastOpenedForUserDTO dto = new LastOpenedForUserDTO("userId", list);

        var result = StartPageService.filterForExistingIdAndCapAtMaximum(dto, grnRegistry.newGRN(GRNTypes.DASHBOARD, "5"), MAX);
        assertThat(result.size()).isEqualTo(8);
    }
}
