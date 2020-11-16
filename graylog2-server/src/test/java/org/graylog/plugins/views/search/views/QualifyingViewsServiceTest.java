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
package org.graylog.plugins.views.search.views;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.ValueParameter;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualifyingViewsServiceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ViewService viewService;

    @Mock
    private SearchDbService searchDbService;

    private QualifyingViewsService qualifyingViewsService;

    @Before
    public void setUp() throws Exception {
        this.qualifyingViewsService = new QualifyingViewsService(searchDbService, viewService);
    }

    @Test
    public void returnEmptyListWhenNoViewsArePresent() {
        when(viewService.streamAll()).then(invocation -> Stream.empty());
        when(searchDbService.findByIds(any())).thenReturn(Collections.emptyList());

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsService.forValue();

        assertThat(result).isEmpty();
    }

    @Test
    public void returnEmptyListWhenNoSearchesWithParametersArePresent() {
        final ViewDTO view1 = mock(ViewDTO.class);
        final Search search = mock(Search.class);
        when(search.parameters()).thenReturn(ImmutableSet.of());
        when(viewService.streamAll()).then(invocation -> Stream.of(view1));
        when(searchDbService.findByIds(any())).thenReturn(ImmutableList.of(search));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsService.forValue();

        assertThat(result).isEmpty();
    }

    @Test
    public void returnViewWhenSearchWithParametersIsPresent() {
        final ViewDTO view1 = mock(ViewDTO.class);
        final String viewId = "viewWithParameter";
        final Search search = mock(Search.class);
        final String searchId = "streamWithParameter";
        when(view1.id()).thenReturn(viewId);
        when(view1.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(view1.searchId()).thenReturn(searchId);
        when(view1.title()).thenReturn("My View");
        when(view1.summary()).thenReturn("My Summary");
        when(view1.description()).thenReturn("My Description");
        when(search.id()).thenReturn(searchId);
        when(search.parameters()).thenReturn(ImmutableSet.of(ValueParameter.any("foobar")));
        when(viewService.streamAll()).then(invocation -> Stream.of(view1));
        when(searchDbService.findByIds(any())).thenReturn(ImmutableList.of(search));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsService.forValue();

        assertThat(result)
                .hasSize(1)
                .allMatch(summary -> summary.id().equals(viewId))
                .allMatch(summary -> summary.title().equals("My View"))
                .allMatch(summary -> summary.summary().equals("My Summary"))
                .allMatch(summary -> summary.description().equals("My Description"));
    }

    @Test
    public void returnViewWhenBothSearchesWithAndWithoutParametersIsPresent() {
        final ViewDTO view1 = mock(ViewDTO.class);
        final ViewDTO view2 = mock(ViewDTO.class);
        final String viewId = "viewWithParameter";
        final Search search1 = mock(Search.class);
        final Search search2 = mock(Search.class);
        final String search1Id = "streamWithParameter";
        when(view1.id()).thenReturn(viewId);
        when(view1.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(view1.searchId()).thenReturn(search1Id);
        when(view1.title()).thenReturn("My View");
        when(view1.summary()).thenReturn("My Summary");
        when(view1.description()).thenReturn("My Description");
        when(view2.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(search1.id()).thenReturn(search1Id);
        when(search1.parameters()).thenReturn(ImmutableSet.of(ValueParameter.any("foobar")));
        when(search2.parameters()).thenReturn(ImmutableSet.of());
        when(viewService.streamAll()).then(invocation -> Stream.of(view1, view2));
        when(searchDbService.findByIds(any())).thenReturn(ImmutableList.of(search1, search2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsService.forValue();

        assertThat(result)
                .hasSize(1)
                .allMatch(summary -> summary.id().equals(viewId))
                .allMatch(summary -> summary.title().equals("My View"))
                .allMatch(summary -> summary.summary().equals("My Summary"))
                .allMatch(summary -> summary.description().equals("My Description"));
    }
}
