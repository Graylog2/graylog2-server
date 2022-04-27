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

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.ValueParameter;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualifyingViewsServiceTest {

    @Test
    public void returnEmptyListWhenNoViewsArePresent() {
        final QualifyingViewsService service = new QualifyingViewsService(mockSearchService(), mockViewService());
        final Collection<ViewParameterSummaryDTO> result = service.forValue();
        assertThat(result).isEmpty();
    }

    @Test
    public void returnEmptyListWhenNoSearchesWithParametersArePresent() {
        final ViewDTO view1 = createView("a-view");

        final Search search = Search.builder()
                .parameters(ImmutableSet.of())
                .build();

        final QualifyingViewsService service = new QualifyingViewsService(mockSearchService(search), mockViewService(view1));
        final Collection<ViewParameterSummaryDTO> result = service.forValue();

        assertThat(result).isEmpty();
    }

    @Test
    public void returnViewWhenSearchWithParametersIsPresent() {
        final Search search = Search.builder()
                .id("streamWithParameter")
                .parameters(ImmutableSet.of(ValueParameter.any("foobar")))
                .build();

        final ViewDTO view1 = createView("streamWithParameter");

        final QualifyingViewsService service = new QualifyingViewsService(mockSearchService(search), mockViewService(view1));
        final Collection<ViewParameterSummaryDTO> result = service.forValue();

        assertThat(result)
                .hasOnlyOneElementSatisfying(summary -> {
                            assertThat(summary.id()).isEqualTo("viewWithParameter");
                            assertThat(summary.title()).isEqualTo("My View");
                            assertThat(summary.summary()).isEqualTo("My Summary");
                            assertThat(summary.description()).isEqualTo("My Description");
                        }
                );
    }

    @Test
    public void returnViewWhenBothSearchesWithAndWithoutParametersIsPresent() {
        final Search search1 = Search.builder()
                .id("streamWithParameter")
                .parameters(ImmutableSet.of(ValueParameter.any("foobar")))
                .build();

        final Search search2 = Search.builder()
                .parameters(ImmutableSet.of())
                .build();

        final ViewDTO view1 = createView("streamWithParameter");
        final ViewDTO view2 = createView("anotherView");

        final QualifyingViewsService service = new QualifyingViewsService(
                mockSearchService(search1, search2),
                mockViewService(view1, view2)
        );

        final Collection<ViewParameterSummaryDTO> result = service.forValue();

        assertThat(result)
                .hasOnlyOneElementSatisfying(summary -> {
                            assertThat(summary.id()).isEqualTo("viewWithParameter");
                            assertThat(summary.title()).isEqualTo("My View");
                            assertThat(summary.summary()).isEqualTo("My Summary");
                            assertThat(summary.description()).isEqualTo("My Description");
                        }
                );
    }

    private ViewDTO createView(String searchId) {
        return ViewDTO.builder()
                .searchId(searchId)
                .id("viewWithParameter")
                .type(ViewDTO.Type.SEARCH)
                .title("My View")
                .summary("My Summary")
                .description("My Description")
                .state(Collections.emptyMap())
                .build();
    }

    private ViewService mockViewService(ViewDTO... viewDTOS) {
        final ViewService mock = mock(ViewService.class);
        when(mock.streamAll()).then(invocation -> Stream.of(viewDTOS));
        return mock;
    }

    private SearchDbService mockSearchService(Search... searches) {
        final SearchDbService mock = mock(SearchDbService.class);
        when(mock.findByIds(any())).thenReturn(Arrays.asList(searches));
        return mock;
    }
}
