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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchfilters.ReferencedSearchFiltersHelper;
import org.graylog.plugins.views.search.searchfilters.db.SearchFilterVisibilityCheckStatus;
import org.graylog.plugins.views.search.searchfilters.db.SearchFilterVisibilityChecker;
import org.graylog.plugins.views.search.searchfilters.model.UsesSearchFilters;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.views.Position;
import org.graylog.plugins.views.search.views.UnknownWidgetConfigDTO;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewResolverDecoder;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewStateDTO;
import org.graylog.plugins.views.search.views.WidgetDTO;
import org.graylog.plugins.views.search.views.WidgetPositionDTO;
import org.graylog.plugins.views.startpage.StartPageService;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.dashboards.events.DashboardDeletedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.security.Permissions;
import org.graylog2.users.UserImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ViewsResourceTest {
    private static final String VIEW_ID = "test-view";
    private static final String SEARCH_ID = "6141d457d3a6b9d73c8ac55a";
    private static final Map<String, ViewResolver> EMPTY_VIEW_RESOLVERS = Collections.emptyMap();
    private static final SearchFilterVisibilityChecker EMPTY_SEARCH_FILTER_VISIBILITY_CHECKER = searchFilterVisibilityChecker(Collections.emptyList());

    private static final SearchUser SEARCH_USER = TestSearchUser.builder()
            .withUser(testUser -> testUser.withUsername("testuser"))
            .allowView(VIEW_ID)
            .allowEditView(VIEW_ID)
            .allowDeleteView(VIEW_ID)
            .allowDeleteView("foobar")
            .canCreateDashboards(true)
            .build();

    private static final ViewDTO TEST_DASHBOARD_VIEW = ViewDTO.builder()
            .id(VIEW_ID)
            .title("test-dashboard")
            .searchId(SEARCH_ID)
            .state(Collections.emptyMap())
            .type(ViewDTO.Type.DASHBOARD)
            .build();

    private static final ViewDTO TEST_SEARCH_VIEW = ViewDTO.builder()
            .id(VIEW_ID)
            .title("test-dashboard")
            .searchId(SEARCH_ID)
            .state(Collections.emptyMap())
            .type(ViewDTO.Type.SEARCH)
            .build();

    private static final Search SEARCH = Search.builder()
            .id(SEARCH_ID)
            .queries(ImmutableSet.of())
            .build();

    @Test
    public void creatingViewAddsCurrentUserAsOwner() throws ValidationException {
        final ViewService viewService = mock(ViewService.class);
        final var dto = ViewDTO.builder().searchId("1").title("2").state(new HashMap<>()).build();
        when(viewService.saveWithOwner(any(), any())).thenReturn(dto);

        final ViewsResource viewsResource = createViewsResource(
                viewService,
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                new ReferencedSearchFiltersHelper(),
                EMPTY_SEARCH_FILTER_VISIBILITY_CHECKER,
                EMPTY_VIEW_RESOLVERS,
                SEARCH
        );


        viewsResource.create(TEST_DASHBOARD_VIEW, mockUserContext(), SEARCH_USER);

        final ArgumentCaptor<ViewDTO> viewCaptor = ArgumentCaptor.forClass(ViewDTO.class);
        final ArgumentCaptor<User> ownerCaptor = ArgumentCaptor.forClass(User.class);

        verify(viewService, times(1)).saveWithOwner(viewCaptor.capture(), ownerCaptor.capture());

        assertThat(viewCaptor.getValue().owner()).hasValue("testuser");
        assertThat(ownerCaptor.getValue().getName()).isEqualTo("testuser");
    }

    @Test
    public void throwsExceptionWhenCreatingSearchWithFilterThatUserIsNotAllowedToSee() {
        final ViewsResource viewsResource = createViewsResource(
                mock(ViewService.class),
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                new ReferencedSearchFiltersHelper(),
                searchFilterVisibilityChecker(Collections.singletonList("<<You cannot see this filter>>")),
                EMPTY_VIEW_RESOLVERS,
                SEARCH
        );

        Assertions.assertThatThrownBy(() -> viewsResource.create(TEST_DASHBOARD_VIEW, mock(UserContext.class), SEARCH_USER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("View cannot be saved, as it contains Search Filters which you are not privileged to view : [<<You cannot see this filter>>]");
    }

    @Test
    public void throwsExceptionWhenCreatingDashboardWithFilterThatUserIsNotAllowedToSee() {
        final ViewsResource viewsResource = createViewsResource(
                mockViewService(TEST_DASHBOARD_VIEW),
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                new ReferencedSearchFiltersHelper(),
                searchFilterVisibilityChecker(Collections.singletonList("<<You cannot see this filter>>")),
                EMPTY_VIEW_RESOLVERS,
                SEARCH
        );

        Assertions.assertThatThrownBy(() -> viewsResource.create(TEST_DASHBOARD_VIEW, mock(UserContext.class), SEARCH_USER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("View cannot be saved, as it contains Search Filters which you are not privileged to view : [<<You cannot see this filter>>]");
    }

    @Test
    public void throwsExceptionWhenUpdatingSearchWithFilterThatUserIsNotAllowedToSee() {
        final ViewsResource viewsResource = createViewsResource(
                mockViewService(TEST_DASHBOARD_VIEW),
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                new ReferencedSearchFiltersHelper(),
                searchFilterVisibilityChecker(Collections.singletonList("<<You cannot see this filter>>")),
                EMPTY_VIEW_RESOLVERS,
                SEARCH
        );

        Assertions.assertThatThrownBy(() -> viewsResource.update(VIEW_ID, TEST_DASHBOARD_VIEW, SEARCH_USER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("View cannot be saved, as it contains Search Filters which you are not privileged to view : [<<You cannot see this filter>>]");
    }

    @Test
    public void updatesSearchSuccessfullyIfInvisibleFilterWasPresentBefore() {
        final ViewService viewService = mockViewService(TEST_SEARCH_VIEW);
        final var dto = ViewDTO.builder().searchId("1").title("2").state(new HashMap<>()).build();
        when(viewService.update(any())).thenReturn(dto);

        final ViewsResource viewsResource = createViewsResource(
                viewService,
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                referencedFiltersHelperWithIDs(Collections.singleton("<<Hidden filter, but not added by this update>>")),
                searchFilterVisibilityChecker(Collections.singletonList("<<Hidden filter, but not added by this update>>")),
                EMPTY_VIEW_RESOLVERS,
                SEARCH
        );

        viewsResource.update(VIEW_ID, TEST_SEARCH_VIEW, SEARCH_USER);
        verify(viewService).update(TEST_SEARCH_VIEW);
    }

    @Test
    public void throwsExceptionWhenUpdatingDashboardWithFilterThatUserIsNotAllowedToSee() {
        final ViewsResource viewsResource = createViewsResource(
                mockViewService(TEST_DASHBOARD_VIEW),
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                new ReferencedSearchFiltersHelper(),
                searchFilterVisibilityChecker(Collections.singletonList("<<You cannot see this filter>>")),
                EMPTY_VIEW_RESOLVERS,
                SEARCH
        );

        Assertions.assertThatThrownBy(() -> viewsResource.update(VIEW_ID, TEST_DASHBOARD_VIEW, SEARCH_USER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("View cannot be saved, as it contains Search Filters which you are not privileged to view : [<<You cannot see this filter>>]");
    }

    @Test
    public void updatesDashboardSuccessfullyIfInvisibleFilterWasPresentBefore() {
        final ViewService viewService = mockViewService(TEST_DASHBOARD_VIEW);
        final var dto = ViewDTO.builder().searchId("1").title("2").state(new HashMap<>()).build();
        when(viewService.update(any())).thenReturn(dto);

        final ViewsResource viewsResource = createViewsResource(
                viewService,
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                referencedFiltersHelperWithIDs(Collections.singleton("<<Hidden filter, but not added by this update>>")),
                searchFilterVisibilityChecker(Collections.singletonList("<<Hidden filter, but not added by this update>>")),
                EMPTY_VIEW_RESOLVERS,
                SEARCH
        );
        viewsResource.update(VIEW_ID, TEST_DASHBOARD_VIEW, SEARCH_USER);
        verify(viewService).update(TEST_DASHBOARD_VIEW);
    }

    @Test
    void testVerifyIntegrity() {
        final ViewsResource viewsResource = createViewsResource(
                mock(ViewService.class),
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                new ReferencedSearchFiltersHelper(),
                EMPTY_SEARCH_FILTER_VISIBILITY_CHECKER,
                EMPTY_VIEW_RESOLVERS
        );

        final ViewDTO view = ViewDTO.builder()
                .searchId("123")
                .title("my-search")
                .state(Collections.emptyMap())
                .build();

        final Search search = Search.builder()
                .id("123")
                .build();

        // empty search, nothing to validate, should succeed
        Assertions.assertThatCode(() -> viewsResource.validateSearchProperties(view, search))
                .doesNotThrowAnyException();

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

    @Test
    public void shouldNotCreateADashboardWithoutPermission() {
        final ViewsResource viewsResource = createViewsResource(
                mock(ViewService.class),
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                new ReferencedSearchFiltersHelper(),
                EMPTY_SEARCH_FILTER_VISIBILITY_CHECKER,
                EMPTY_VIEW_RESOLVERS
        );

        final SearchUser user = TestSearchUser.builder()
                .canCreateDashboards(false)
                .build();

        assertThatThrownBy(() -> viewsResource.create(TEST_DASHBOARD_VIEW, mock(UserContext.class), user))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void invalidObjectIdReturnsViewNotFoundException() {
        final ViewsResource viewsResource = createViewsResource(
                mock(ViewService.class),
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                new ReferencedSearchFiltersHelper(),
                EMPTY_SEARCH_FILTER_VISIBILITY_CHECKER,
                EMPTY_VIEW_RESOLVERS
        );

        assertThatThrownBy(() -> viewsResource.get("invalid", SEARCH_USER))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void deletingDashboardTriggersEvent() {
        final ViewService viewService = mock(ViewService.class);
        final ClusterEventBus clusterEventBus = mock(ClusterEventBus.class);

        final ViewDTO view = ViewDTO.builder()
                .id("foobar")
                .type(ViewDTO.Type.DASHBOARD)
                .searchId(SEARCH_ID)
                .title("my-dashboard")
                .state(Collections.emptyMap())
                .build();

        when(viewService.get("foobar")).thenReturn(Optional.of(view));


        final ViewsResource viewsResource = createViewsResource(
                viewService,
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                clusterEventBus,
                new ReferencedSearchFiltersHelper(),
                EMPTY_SEARCH_FILTER_VISIBILITY_CHECKER,
                EMPTY_VIEW_RESOLVERS
        );

        viewsResource.delete("foobar", SEARCH_USER);

        final ArgumentCaptor<DashboardDeletedEvent> eventCaptor = ArgumentCaptor.forClass(DashboardDeletedEvent.class);
        verify(clusterEventBus, times(1)).post(eventCaptor.capture());
        final DashboardDeletedEvent dashboardDeletedEvent = eventCaptor.getValue();

        assertThat(dashboardDeletedEvent.dashboardId()).isEqualTo("foobar");
    }

    @Test
    public void testViewResolver() {
        // Setup
        final String resolverName = "test-resolver";
        final Map<String, ViewResolver> viewResolvers = new HashMap<>();
        viewResolvers.put(resolverName, new TestableFixedViewResolver(TEST_DASHBOARD_VIEW));

        final ViewsResource testResource = createViewsResource(
                mock(ViewService.class),
                mock(StartPageService.class),
                mock(RecentActivityService.class),
                mock(ClusterEventBus.class),
                new ReferencedSearchFiltersHelper(),
                EMPTY_SEARCH_FILTER_VISIBILITY_CHECKER,
                viewResolvers
        );

        // Verify that view for valid id is found.
        Assertions.assertThat(testResource.get(resolverName + ViewResolverDecoder.SEPARATOR + VIEW_ID, SEARCH_USER).id()).isEqualTo(VIEW_ID);


        // Verify error paths for invalid resolver names and view ids.
        Assertions.assertThatThrownBy(() -> testResource.get("invalid-resolver-name" + ViewResolverDecoder.SEPARATOR + VIEW_ID, SEARCH_USER))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Failed to find view resolver: invalid-resolver-name");

        Assertions.assertThatThrownBy(() -> testResource.get(resolverName + ViewResolverDecoder.SEPARATOR + "invalid-view-id", SEARCH_USER))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Failed to resolve view:test-resolver__invalid-view-id");
    }

    private ViewsResource createViewsResource(final ViewService viewService, final StartPageService startPageService, final RecentActivityService recentActivityService, final ClusterEventBus clusterEventBus, ReferencedSearchFiltersHelper referencedSearchFiltersHelper, SearchFilterVisibilityChecker searchFilterVisibilityChecker, final Map<String, ViewResolver> viewResolvers, Search... existingSearches) {

        final SearchDomain searchDomain = mock(SearchDomain.class);
        for (Search search : existingSearches) {
            when(searchDomain.getForUser(eq(search.id()), eq(SEARCH_USER))).thenReturn(Optional.of(search));
        }

        return new ViewsResource(viewService, startPageService, recentActivityService, clusterEventBus, searchDomain, viewResolvers, searchFilterVisibilityChecker, referencedSearchFiltersHelper, mock(AuditEventSender.class), mock(ObjectMapper.class)) {
            @Override
            protected Subject getSubject() {
                return mock(Subject.class);
            }

            @Override
            protected User getCurrentUser() {
                return mock(User.class);
            }
        };
    }

    private static ViewService mockViewService(ViewDTO existingView) {
        final ViewService viewService = mock(ViewService.class);
        when(viewService.get(existingView.id())).thenReturn(Optional.of(existingView));
        return viewService;
    }


    private UserContext mockUserContext() {
        final UserImpl testUser = new UserImpl(mock(PasswordAlgorithmFactory.class), new Permissions(ImmutableSet.of()),
                mock(ClusterConfigService.class), ImmutableMap.of("username", "testuser"));
        final UserContext userContext = mock(UserContext.class);
        when(userContext.getUser()).thenReturn(testUser);
        return userContext;
    }

    private static ReferencedSearchFiltersHelper referencedFiltersHelperWithIDs(final Set<String> ids) {
        return new ReferencedSearchFiltersHelper() {
            @Override
            public Set<String> getReferencedSearchFiltersIds(Collection<UsesSearchFilters> searchFiltersOwners) {
                return ids;
            }
        };
    }

    private static SearchFilterVisibilityChecker searchFilterVisibilityChecker(List<String> hiddenSearchFilterIds) {
        return new SearchFilterVisibilityChecker() {
            @Override
            public SearchFilterVisibilityCheckStatus checkSearchFilterVisibility(Predicate<String> readPermissionPredicate, Collection<String> referencedSearchFiltersIds) {
                return new SearchFilterVisibilityCheckStatus(hiddenSearchFilterIds);
            }
        };
    }
}
