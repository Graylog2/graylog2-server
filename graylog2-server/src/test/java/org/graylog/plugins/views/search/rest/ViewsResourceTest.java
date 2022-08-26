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
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchfilters.ReferencedSearchFiltersHelper;
import org.graylog.plugins.views.search.searchfilters.db.SearchFilterVisibilityCheckStatus;
import org.graylog.plugins.views.search.searchfilters.db.SearchFilterVisibilityChecker;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.UserContext;
import org.graylog2.dashboards.events.DashboardDeletedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserImpl;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.WARN)
public class ViewsResourceTest {
    public static final String VIEW_ID = "test-view";
    public static final String SEARCH_ID = "6141d457d3a6b9d73c8ac55a";

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

    private Search search;

    @Mock
    private SearchFilterVisibilityChecker searchFilterVisibilityChecker;

    @Mock
    private ReferencedSearchFiltersHelper referencedSearchFiltersHelper;

    private ViewsResource viewsResource;

    @BeforeEach
    public void setUp() {
        this.viewsResource = new ViewsTestResource(viewService, clusterEventBus, userService, searchDomain, referencedSearchFiltersHelper);
        when(searchUser.canCreateDashboards()).thenReturn(true);
        this.search = mock(Search.class, RETURNS_DEEP_STUBS);
        doReturn(SEARCH_ID).when(search).id();
        when(search.queries()).thenReturn(ImmutableSet.of());
        when(searchDomain.getForUser(eq(SEARCH_ID), eq(searchUser))).thenReturn(Optional.of(search));
        when(viewService.get(VIEW_ID)).thenReturn(Optional.of(view));
        when(searchFilterVisibilityChecker.checkSearchFilterVisibility(any(), any())).thenReturn(new SearchFilterVisibilityCheckStatus());
    }

    class ViewsTestResource extends ViewsResource {
        ViewsTestResource(ViewService viewService, ClusterEventBus clusterEventBus, UserService userService, SearchDomain searchDomain, ReferencedSearchFiltersHelper referencedSearchFiltersHelper) {
            this(viewService, clusterEventBus, userService, searchDomain, new HashMap<>(), referencedSearchFiltersHelper);
        }

        ViewsTestResource(ViewService viewService, ClusterEventBus clusterEventBus, UserService userService, SearchDomain searchDomain, Map<String, ViewResolver> viewResolvers, ReferencedSearchFiltersHelper referencedSearchFiltersHelper) {
            super(viewService, clusterEventBus, searchDomain, viewResolvers, searchFilterVisibilityChecker, referencedSearchFiltersHelper);
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

    @Test
    public void creatingViewAddsCurrentUserAsOwner() throws Exception {
        final ViewDTO.Builder builder = mockView(ViewDTO.Type.DASHBOARD, view);
        final UserContext userContext = mockUser();
        this.viewsResource.create(view, userContext, searchUser);

        final ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
        verify(builder, times(1)).owner(ownerCaptor.capture());
        assertThat(ownerCaptor.getValue()).isEqualTo("testuser");
    }

    @Test
    public void throwsExceptionWhenCreatingSearchWithFilterThatUserIsNotAllowedToSee() throws Exception {
        mockView(ViewDTO.Type.SEARCH, view);
        final UserContext userContext = mockUser();
        doReturn(new SearchFilterVisibilityCheckStatus(Collections.singletonList("<<You cannot see this filter>>")))
                .when(searchFilterVisibilityChecker)
                .checkSearchFilterVisibility(any(), any());

        Assert.assertThrows(BadRequestException.class, () -> this.viewsResource.create(view, userContext, searchUser));
    }

    @Test
    public void throwsExceptionWhenCreatingDashboardWithFilterThatUserIsNotAllowedToSee() throws Exception {
        mockView(ViewDTO.Type.DASHBOARD, view);
        final UserContext userContext = mockUser();
        doReturn(new SearchFilterVisibilityCheckStatus(Collections.singletonList("<<You cannot see this filter>>")))
                .when(searchFilterVisibilityChecker)
                .checkSearchFilterVisibility(any(), any());

        Assert.assertThrows(BadRequestException.class, () -> this.viewsResource.create(view, userContext, searchUser));
    }

    @Test
    public void throwsExceptionWhenUpdatingSearchWithFilterThatUserIsNotAllowedToSee() throws Exception {
        prepareUpdate(ViewDTO.Type.SEARCH);
        doReturn(new SearchFilterVisibilityCheckStatus(Collections.singletonList("<<You cannot see this filter>>")))
                .when(searchFilterVisibilityChecker)
                .checkSearchFilterVisibility(any(), any());

        Assert.assertThrows(BadRequestException.class, () -> this.viewsResource.update(VIEW_ID, view, searchUser));
    }

    @Test
    public void updatesSearchSuccessfullyIfInvisibleFilterWasPresentBefore() throws Exception {
        prepareUpdate(ViewDTO.Type.SEARCH);
        doReturn(Collections.singleton("<<Hidden filter, but not added by this update>>")).when(referencedSearchFiltersHelper).getReferencedSearchFiltersIds(any());
        doReturn(new SearchFilterVisibilityCheckStatus(Collections.singletonList("<<Hidden filter, but not added by this update>>")))
                .when(searchFilterVisibilityChecker)
                .checkSearchFilterVisibility(any(), any());

        this.viewsResource.update(VIEW_ID, view, searchUser);
        verify(viewService).update(view);
    }

    @Test
    public void throwsExceptionWhenUpdatingDashboardWithFilterThatUserIsNotAllowedToSee() throws Exception {
        prepareUpdate(ViewDTO.Type.DASHBOARD);
        doReturn(Collections.emptySet()).when(referencedSearchFiltersHelper).getReferencedSearchFiltersIds(any());
        doReturn(new SearchFilterVisibilityCheckStatus(Collections.singletonList("<<You cannot see this filter>>")))
                .when(searchFilterVisibilityChecker)
                .checkSearchFilterVisibility(any(), any());

        Assert.assertThrows(BadRequestException.class, () -> this.viewsResource.update(VIEW_ID, view, searchUser));
    }

    @Test
    public void updatesDashboardSuccessfullyIfInvisibleFilterWasPresentBefore() throws Exception {
        prepareUpdate(ViewDTO.Type.DASHBOARD);
        doReturn(Collections.singleton("<<Hidden filter, but not added by this update>>")).when(referencedSearchFiltersHelper).getReferencedSearchFiltersIds(any());
        doReturn(new SearchFilterVisibilityCheckStatus(Collections.singletonList("<<Hidden filter, but not added by this update>>")))
                .when(searchFilterVisibilityChecker)
                .checkSearchFilterVisibility(any(), any());

        this.viewsResource.update(VIEW_ID, view, searchUser);
        verify(viewService).update(view);
    }

    private void prepareUpdate(final ViewDTO.Type viewType) {
        this.viewsResource = spy(new ViewsTestResource(viewService, clusterEventBus, userService, searchDomain, referencedSearchFiltersHelper));

        ViewDTO originalView = mock(ViewDTO.class);
        final ViewDTO.Builder originalViewBuilder = mockView(viewType, originalView);
        originalViewBuilder.id(VIEW_ID);
        when(originalView.id()).thenReturn(VIEW_ID);
        doReturn(originalView).when(viewsResource).resolveView(VIEW_ID);

        final ViewDTO.Builder builder = mockView(viewType, view);
        builder.id(VIEW_ID);
        when(view.id()).thenReturn(VIEW_ID);
        mockUser();
    }

    private UserContext mockUser() {
        final UserImpl testUser = new UserImpl(mock(PasswordAlgorithmFactory.class), new Permissions(ImmutableSet.of()), ImmutableMap.of("username", "testuser"));

        final UserContext userContext = mock(UserContext.class);
        when(userContext.getUser()).thenReturn(testUser);
        when(searchUser.username()).thenReturn("testuser");
        when(searchUser.canUpdateView(any())).thenReturn(true);
        return userContext;
    }

    private ViewDTO.Builder mockView(final ViewDTO.Type type, final ViewDTO view) {
        final ViewDTO.Builder builder = mock(ViewDTO.Builder.class);

        when(view.toBuilder()).thenReturn(builder);
        when(view.type()).thenReturn(type);
        when(view.searchId()).thenReturn(SEARCH_ID);
        when(builder.owner(any())).thenReturn(builder);
        when(builder.id(any())).thenReturn(builder);
        when(builder.build()).thenReturn(view);
        return builder;
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
        assertThatThrownBy(() -> this.viewsResource.get("invalid", searchUser))
                .isInstanceOf(NotFoundException.class);
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
        final ViewsResource testResource = new ViewsTestResource(viewService, clusterEventBus, userService, searchDomain, viewResolvers, referencedSearchFiltersHelper);

        // Verify that view for valid id is found.
        when(searchUser.canReadView(any())).thenReturn(true);
        assertEquals(VIEW_ID, testResource.get(resolverName + ":" + VIEW_ID, searchUser).id());


        // Verify error paths for invalid resolver names and view ids.
        assertThrows(NotFoundException.class,
                () -> testResource.get("invalid-resolver-name:" + VIEW_ID, searchUser));
        assertThrows(NotFoundException.class,
                () -> testResource.get(resolverName + ":invalid-view-id", searchUser));
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
