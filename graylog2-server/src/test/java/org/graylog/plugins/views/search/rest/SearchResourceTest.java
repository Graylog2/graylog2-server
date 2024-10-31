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

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchResourceTest {


    private final SearchUser searchUser = TestSearchUser.builder().build();
    @Mock
    private SearchJobService searchJobService;
    @Mock
    private EventBus eventBus;
    @Mock
    private SearchExecutor searchExecutor;

    @Mock
    private ClusterConfigService clusterConfigService;

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");


    @Test
    public void testBuilderGeneratesSearchId() {
        final Search search = Search.builder().build();
        assertThat(search.id()).isNotNull();
        assertThat(org.bson.types.ObjectId.isValid(search.id())).isTrue();
    }

    @Test
    public void getSearchLoadsSearch() {
        final Query query = Query.builder()
                .id("queryId")
                .searchTypes(Collections.emptySet())
                .filter(StreamFilter.anyIdOf("streamId"))
                .build();

        final Search search = Search.builder()
                .id("deadbeef")
                .parameters(ImmutableSet.of())
                .queries(ImmutableSet.of(query))
                .build();

        final SearchDomain searchDomain = mockSearchDomain(Optional.of(search));
        final SearchResource resource = new SearchResource(searchDomain, searchExecutor, searchJobService, eventBus, clusterConfigService);
        final SearchDTO returnedSearch = resource.getSearch(search.id(), searchUser);

        assertThat(returnedSearch.id()).isEqualTo(search.id());
    }

    @Test
    public void getSearchThrowsNotFoundIfSearchDoesntExist() {
        final SearchDomain searchDomain = mockSearchDomain(Optional.empty());
        final SearchResource resource = new SearchResource(searchDomain, searchExecutor, searchJobService, eventBus, clusterConfigService);
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> resource.getSearch("god", searchUser))
                .withMessageContaining("god");
    }

    @Test
    public void allowCreatingNewSearchWithoutId() {
        final SearchDTO search = SearchDTO.Builder.create().id(null).build();

        final SearchDomain searchDomain = mock(SearchDomain.class);
        when(searchDomain.saveForUser(any(), any())).thenReturn(search.toSearch());

        final SearchResource resource = new SearchResource(searchDomain, searchExecutor, searchJobService, eventBus, clusterConfigService);
        final Response response = resource.createSearch(search, searchUser);

        Assertions.assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private SearchDomain mockSearchDomain(Optional<Search> search) {
        final SearchDomain searchDomain = mock(SearchDomain.class);
        search.ifPresentOrElse(
                s -> when(searchDomain.getForUser(eq(s.id()), any())).thenReturn(search),
                () -> when(searchDomain.getForUser(any(), any())).thenReturn(Optional.empty()));
        return searchDomain;
    }
}
