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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.views.Position;
import org.graylog.plugins.views.search.views.UnknownWidgetConfigDTO;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewStateDTO;
import org.graylog.plugins.views.search.views.WidgetDTO;
import org.graylog.plugins.views.search.views.WidgetPositionDTO;
import org.graylog.security.UserContext;
import org.graylog2.dashboards.events.DashboardDeletedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ViewsResourceTest {
    public static final String VIEW_ID = "test-view";

    @Before
    public void setUpInjector() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Subject subject;

    @Mock
    private User currentUser;

    @Mock
    private SearchUser searchUser;

    @Mock
    private ViewService viewService;

    @Mock
    private ViewDTO view;

    @Mock
    private ClusterEventBus clusterEventBus;

    @Mock
    private UserService userService;

    @Mock
    private SearchDomain searchDomain;

    private ViewsResource viewsResource;

    class ViewsTestResource extends ViewsResource {
        ViewsTestResource(ViewService viewService, ClusterEventBus clusterEventBus, UserService userService, SearchDomain searchDomain) {
            this(viewService, clusterEventBus, userService, searchDomain, new HashMap<>());
        }

        ViewsTestResource(ViewService viewService, ClusterEventBus clusterEventBus, UserService userService, SearchDomain searchDomain, Map<String, ViewResolver> viewResolvers) {
            super(viewService, clusterEventBus, searchDomain, viewResolvers);
            this.userService = userService;
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }

        @Nullable
        @Override
        protected User getCurrentUser() {
            return currentUser;
        }
    }

    @Before
    public void setUp() throws Exception {
        this.viewsResource = new ViewsTestResource(viewService, clusterEventBus, userService, searchDomain);
        when(searchUser.canCreateDashboards()).thenReturn(true);
        final Search search = mock(Search.class, RETURNS_DEEP_STUBS);
        when(search.queries()).thenReturn(ImmutableSet.of());
        when(searchDomain.getForUser(eq("6141d457d3a6b9d73c8ac55a"), eq(searchUser))).thenReturn(Optional.of(search));
    }

    @Test
    public void creatingViewAddsCurrentUserAsOwner() throws Exception {
        final ViewDTO.Builder builder = mock(ViewDTO.Builder.class);

        when(view.toBuilder()).thenReturn(builder);
        when(view.type()).thenReturn(ViewDTO.Type.DASHBOARD);
        when(view.searchId()).thenReturn("6141d457d3a6b9d73c8ac55a");
        when(builder.owner(any())).thenReturn(builder);
        when(builder.build()).thenReturn(view);

        final UserImpl testUser = new UserImpl(mock(PasswordAlgorithmFactory.class), new Permissions(ImmutableSet.of()), ImmutableMap.of("username", "testuser"));

        final UserContext userContext = mock(UserContext.class);
        when(userContext.getUser()).thenReturn(testUser);
        when(userContext.getUserId()).thenReturn("testuser");
        when(currentUser.isLocalAdmin()).thenReturn(true);
        when(searchUser.username()).thenReturn("testuser");

        this.viewsResource.create(view, userContext, searchUser);

        final ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
        verify(builder, times(1)).owner(ownerCaptor.capture());
        assertThat(ownerCaptor.getValue()).isEqualTo("testuser");
    }

    @Test
    public void shouldNotCreateADashboardWithoutPermission() {
        when(view.type()).thenReturn(ViewDTO.Type.DASHBOARD);

        when(searchUser.canCreateDashboards()).thenReturn(false);

        assertThatThrownBy(() -> this.viewsResource.create(view, null, searchUser))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void invalidObjectIdReturnsViewNotFoundException() {
        expectedException.expect(NotFoundException.class);
        this.viewsResource.get("invalid", searchUser);
    }

    @Test
    public void deletingDashboardTriggersEvent() {
        final String viewId = "foobar";
        when(searchUser.canDeleteView(view)).thenReturn(true);
        when(view.type()).thenReturn(ViewDTO.Type.DASHBOARD);
        when(view.id()).thenReturn(viewId);
        when(viewService.get(viewId)).thenReturn(Optional.of(view));
        when(userService.loadAll()).thenReturn(Collections.emptyList());

        this.viewsResource.delete(viewId, searchUser);

        final ArgumentCaptor<DashboardDeletedEvent> eventCaptor = ArgumentCaptor.forClass(DashboardDeletedEvent.class);
        verify(clusterEventBus, times(1)).post(eventCaptor.capture());
        final DashboardDeletedEvent dashboardDeletedEvent = eventCaptor.getValue();

        assertThat(dashboardDeletedEvent.dashboardId()).isEqualTo("foobar");
    }

    @Test
    public void testViewResolver() {
        // Setup
        when(view.id()).thenReturn(VIEW_ID);
        final String resolverName = "test-resolver";
        final Map<String, ViewResolver> viewResolvers = new HashMap<>();
        viewResolvers.put(resolverName, new TestViewResolver());
        final ViewsResource testResource = new ViewsTestResource(viewService, clusterEventBus, userService, searchDomain, viewResolvers);

        // Verify that view for valid id is found.
        when(searchUser.canReadView(any())).thenReturn(true);
        assertEquals(VIEW_ID, testResource.get(resolverName + ":" + VIEW_ID, searchUser).id());


        // Verify error paths for invalid resolver names and view ids.
        assertThrows(NotFoundException.class,
                () -> testResource.get("invalid-resolver-name:" + VIEW_ID, searchUser));
        assertThrows(NotFoundException.class,
                () -> testResource.get(resolverName + ":invalid-view-id", searchUser));
    }

    @Test
    void testVerifyIntegrity() {
        final ViewDTO view = ViewDTO.builder()
                .searchId("123")
                .title("my-search")
                .state(Collections.emptyMap())
                .build();

        final Search search = Search.builder()
                .id("123")
                .build();

        // empty search, nothing to validate, should succeed
        assertDoesNotThrow(() -> viewsResource.validateSearchProperties(view, search));

        final Search searchWithQuery = search.toBuilder().queries(ImmutableSet.of(Query.builder().id("Q-111").build())).build();
        final ViewDTO viewWithQuery = view.toBuilder().state(Collections.singletonMap("Q-123", ViewStateDTO.builder()
                        .widgets(Collections.emptySet())
                        .widgetMapping(Collections.emptyMap())
                        .widgetPositions(Collections.emptyMap())
                        .build()))
                .build();

        // the query with ID Q-123 is present in the view  state, but the search itself doesn't have it => invalid combination
        assertThatThrownBy(() -> viewsResource.validateSearchProperties(viewWithQuery, searchWithQuery))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Search queries do not correspond to view/state queries, missing query IDs: [Q-123]; search queries: [Q-111]; state queries: [Q-123]");

        final Search searchWithTypes = search.toBuilder().queries(ImmutableSet.of(Query.builder().id("Q-111")
                .searchTypes(ImmutableSet.of(MessageList.builder().id("T-111").build()))
                .build())).build();

        final ViewDTO viewWithWidgets = view.toBuilder().state(Collections.singletonMap("Q-111", ViewStateDTO.builder()
                .widgetMapping(Collections.singletonMap("W-123", Collections.singleton("T-123")))
                .widgetPositions(Collections.emptyMap())
                .widgets(Collections.emptySet())
                .build())).build();

        // view contains widget mappings for type T-123, but the search knows only a type T-111 => invalid
        assertThatThrownBy(() -> viewsResource.validateSearchProperties(viewWithWidgets, searchWithTypes))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("missing searches [T-123]; search types: [T-111]; state types: [T-123]");


        final Search searchWithValidTypes = search.toBuilder().queries(ImmutableSet.of(Query.builder().id("Q-111")
                .searchTypes(ImmutableSet.of(MessageList.builder().id("T-123").build()))
                .build())).build();

        final ViewDTO viewWithWidgetPositions = view.toBuilder()
                .state(Collections.singletonMap("Q-111", ViewStateDTO.builder()
                        .widgets(Collections.singleton(WidgetDTO.builder()
                                .id("W-123")
                                .type("my-type")
                                .config(UnknownWidgetConfigDTO.create(Collections.emptyMap()))
                                .build()))
                        .widgetPositions(Collections.singletonMap("W-111",
                                WidgetPositionDTO.Builder.create()
                                        .col(Position.fromInt(1))
                                        .row(Position.fromInt(1))
                                        .height(Position.fromInt(100))
                                        .width(Position.fromInt(100))
                                        .build()))
                        .widgetMapping(Collections.singletonMap("W-123", Collections.singleton("T-123")))
                        .build())).build();

        assertThatThrownBy(() -> viewsResource.validateSearchProperties(viewWithWidgetPositions, searchWithValidTypes))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Widget positions don't correspond to widgets, missing widget positions [W-123]; widget IDs: [W-123]; widget positions: [W-111]");



    }

    class TestViewResolver implements ViewResolver {
        @Override
        public Optional<ViewDTO> get(String id) {
            return id.equals(VIEW_ID) ? Optional.of(view) : Optional.empty();
        }

        @Override
        public Set<String> getSearchIds() {
            return null;
        }

        @Override
        public Set<ViewDTO> getBySearchId(String searchId) {
            return Collections.emptySet();
        }

        @Override
        public boolean canReadView(String viewId, Predicate<String> permissionTester, BiPredicate<String, String> entityPermissionsTester) {
            // Not used in this test.
            return false;
        }
    }
}
