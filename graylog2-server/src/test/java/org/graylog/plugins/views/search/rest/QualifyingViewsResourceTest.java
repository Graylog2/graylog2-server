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
import org.graylog.plugins.views.search.views.QualifyingViewsService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewParameterSummaryDTO;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualifyingViewsResourceTest {

    @Test
    public void returnsNoViewsIfNoneArePresent() {
        final SearchUser searchUser = TestSearchUser.builder().build();

        QualifyingViewsService service = mockViewsService();
        final QualifyingViewsResource resource = new QualifyingViewsResource(service);

        final Collection<ViewParameterSummaryDTO> result = resource.forParameter(searchUser);
        assertThat(result).isEmpty();
    }

    @Test
    public void returnsNoViewsIfNoneArePermitted() {
        final SearchUser searchUser = TestSearchUser.builder()
                .denyView("view1")
                .denyView("view2")
                .build();

        final QualifyingViewsService service = mockViewsService("view1", "view2");
        final QualifyingViewsResource resource = new QualifyingViewsResource(service);
        final Collection<ViewParameterSummaryDTO> result = resource.forParameter(searchUser);

        assertThat(result).isEmpty();
    }

    @Test
    public void returnsSomeViewsIfSomeArePermitted() {

        final SearchUser searchUser = TestSearchUser.builder()
                .denyView("view1")
                .allowView("view2")
                .build();

        final QualifyingViewsService service = mockViewsService("view1", "view2");

        final QualifyingViewsResource resource = new QualifyingViewsResource(service);
        final Collection<ViewParameterSummaryDTO> result = resource.forParameter(searchUser);

        assertThat(result)
                .hasSize(1)
                .extracting(ViewParameterSummaryDTO::id)
                .containsOnly("view2");
    }

    @Test
    public void returnsAllViewsIfAllArePermitted() {

        final SearchUser searchUser = TestSearchUser.builder()
                .allowView("view1")
                .allowView("view2")
                .build();

        final QualifyingViewsService service = mockViewsService("view1", "view2");

        final QualifyingViewsResource resource = new QualifyingViewsResource(service);
        final Collection<ViewParameterSummaryDTO> result = resource.forParameter(searchUser);

        assertThat(result)
                .hasSize(2)
                .extracting(ViewParameterSummaryDTO::id)
                .containsOnly("view1", "view2");
    }

    private QualifyingViewsService mockViewsService(String... viewIDs) {
        final QualifyingViewsService service = mock(QualifyingViewsService.class);
        final List<ViewParameterSummaryDTO> views = Stream.of(viewIDs).map(this::createView).collect(Collectors.toList());
        Mockito.when(service.forValue()).thenReturn(views);
        return service;
    }

    private ViewParameterSummaryDTO createView(String id) {
        final ViewParameterSummaryDTO view = mock(ViewParameterSummaryDTO.class);
        when(view.id()).thenReturn(id);
        when(view.type()).thenReturn(ViewDTO.Type.SEARCH);
        return view;
    }
}
