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
import org.graylog.plugins.views.search.SearchRequiresParameterSupport;
import org.graylog.plugins.views.search.ValueParameter;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
public class RequiresParameterSupportTest {

    @Mock
    private SearchDbService searchDbService;

    private RequiresParameterSupport requiresParameterSupport;

    private ViewDTO view;

    @BeforeEach
    public void setUp() throws Exception {
        this.requiresParameterSupport = new RequiresParameterSupport(searchDbService, new SearchRequiresParameterSupport(new EnterpriseMetadataSummary()));

        this.view = ViewDTO.builder()
                .title("Sample View")
                .state(Collections.emptyMap())
                .searchId("searchId")
                .build();
    }

    @Test
    public void throwsExceptionIfSearchIsMissing() {
        when(searchDbService.get("searchId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.requiresParameterSupport.test(view))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Search searchId for view")
                .hasMessageEndingWith("is missing.");
    }

    @Test
    public void returnsEmptyCapabilitiesIfViewDoesNotHaveParameters() {
        final Search search = Search.builder().parameters(ImmutableSet.of()).build();

        when(searchDbService.get("searchId")).thenReturn(Optional.of(search));

        final Map<String, PluginMetadataSummary> result = this.requiresParameterSupport.test(view);

        assertThat(result).isEmpty();
    }

    @Test
    public void returnsParameterCapabilityIfViewDoesHaveParameters() {
        final Search search = Search.builder().parameters(ImmutableSet.of(
                ValueParameter.builder()
                        .name("foo")
                        .dataType("any")
                        .build()
        )).build();

        when(searchDbService.get("searchId")).thenReturn(Optional.of(search));

        final Map<String, PluginMetadataSummary> result = this.requiresParameterSupport.test(view);

        assertThat(result).containsExactly(
                new AbstractMap.SimpleEntry("parameters", new EnterpriseMetadataSummary())
        );
    }
}
