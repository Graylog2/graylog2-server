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

import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewSummaryDTO;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.rest.models.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardsResourceTest {
    private final ViewSummaryDTO mutableDashboard = dashboard(DefaultEntityScope.NAME);
    private final ViewSummaryDTO immutableDashboard = dashboard(ImmutableSystemScope.NAME);

    private ViewService viewService;
    private SearchUser searchUser;
    private DashboardsResource resource;

    @BeforeEach
    void setUp() {
        viewService = mock(ViewService.class);
        searchUser = mock(SearchUser.class);
        when(searchUser.canReadView(any())).thenReturn(true);
        when(searchUser.canUpdateView(any())).thenReturn(true);
        when(viewService.searchSummariesPaginatedByType(any(), eq(ViewDTO.Type.DASHBOARD), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(PaginatedList.emptyList(1, 50));
        resource = new DashboardsResource(viewService, new EntityScopeService(Set.of(new DefaultEntityScope(), new ImmutableSystemScope())));
    }

    @Test
    void updateScopeExcludesDashboardsInImmutableScope() {
        final Predicate<ViewSummaryDTO> predicate = listWithScope(DashboardsResource.Scope.UPDATE);

        assertThat(predicate.test(mutableDashboard)).isTrue();
        assertThat(predicate.test(immutableDashboard)).isFalse();
    }

    @Test
    void readScopeIncludesDashboardsInImmutableScope() {
        final Predicate<ViewSummaryDTO> predicate = listWithScope(DashboardsResource.Scope.READ);

        assertThat(predicate.test(mutableDashboard)).isTrue();
        assertThat(predicate.test(immutableDashboard)).isTrue();
    }

    @SuppressWarnings("unchecked")
    private Predicate<ViewSummaryDTO> listWithScope(DashboardsResource.Scope scope) {
        resource.views(1, 50, ViewDTO.FIELD_TITLE, SortOrder.ASCENDING, "", List.of(), scope, searchUser);

        final ArgumentCaptor<Predicate<ViewSummaryDTO>> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(viewService).searchSummariesPaginatedByType(any(), eq(ViewDTO.Type.DASHBOARD), any(), predicateCaptor.capture(), any(), anyString(), anyInt(), anyInt());
        return predicateCaptor.getValue();
    }

    private static ViewSummaryDTO dashboard(String scope) {
        return ViewSummaryDTO.builder()
                .id("5def958063303ae5f68eccae")
                .scope(scope)
                .title("Dashboard")
                .searchId("search-id")
                .build();
    }
}
