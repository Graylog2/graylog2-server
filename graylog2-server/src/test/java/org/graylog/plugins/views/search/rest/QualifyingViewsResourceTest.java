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

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.QualifyingViewsService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewParameterSummaryDTO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualifyingViewsResourceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private QualifyingViewsService qualifyingViewsService;

    @Mock
    private SearchUser searchUser;

    private QualifyingViewsResource qualifyingViewsResource;

    @Before
    public void setUp() throws Exception {
        this.qualifyingViewsResource = new QualifyingViewsResource(qualifyingViewsService);
    }

    @Test
    public void returnsNoViewsIfNoneArePresent() {
        when(qualifyingViewsService.forValue()).thenReturn(Collections.emptyList());

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter(searchUser);

        assertThat(result).isEmpty();
    }

    @Test
    public void returnsNoViewsIfNoneArePermitted() {
        final ViewParameterSummaryDTO view1 = mock(ViewParameterSummaryDTO.class);
        when(view1.id()).thenReturn("view1");
        when(view1.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(searchUser.canRead(eq(view1))).thenReturn(false);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(view2.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(searchUser.canRead(eq(view2))).thenReturn(false);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter(searchUser);

        assertThat(result).isEmpty();
    }

    @Test
    public void returnsSomeViewsIfSomeArePermitted() {
        final ViewParameterSummaryDTO view1 = mock(ViewParameterSummaryDTO.class);
        when(view1.id()).thenReturn("view1");
        when(view1.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(searchUser.canRead(eq(view1))).thenReturn(false);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(view2.type()).thenReturn(ViewDTO.Type.SEARCH);
        when(searchUser.canRead(eq(view2))).thenReturn(true);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter(searchUser);

        assertThat(result).containsExactly(view2);
    }

    @Test
    public void returnsAllViewsIfAllArePermitted() {
        final ViewParameterSummaryDTO view1 = mock(ViewParameterSummaryDTO.class);
        when(view1.id()).thenReturn("view1");
        when(searchUser.canRead(eq(view1))).thenReturn(true);
        final ViewParameterSummaryDTO view2 = mock(ViewParameterSummaryDTO.class);
        when(view2.id()).thenReturn("view2");
        when(searchUser.canRead(eq(view2))).thenReturn(true);
        when(qualifyingViewsService.forValue()).thenReturn(ImmutableList.of(view1, view2));

        final Collection<ViewParameterSummaryDTO> result = this.qualifyingViewsResource.forParameter(searchUser);

        assertThat(result).contains(view1, view2);
    }
}
