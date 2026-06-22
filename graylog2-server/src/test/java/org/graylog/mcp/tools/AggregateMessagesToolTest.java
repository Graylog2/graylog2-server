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
package org.graylog.mcp.tools;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.ScriptingApiService;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.Metadata;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AggregateMessagesToolTest {
    private static final List<Map<String, Object>> GROUPINGS = List.of(Map.of("field", "source", "limit", 10));
    private static final List<Map<String, Object>> METRICS = List.of(Map.of("function", "count"));

    private final ScriptingApiService scriptingApiService = mock(ScriptingApiService.class);
    private final PermissionHelper permissionHelper = mock(PermissionHelper.class);
    private final SearchUser searchUser = mock(SearchUser.class);
    private final AggregateMessagesTool tool = new AggregateMessagesTool(scriptingApiService, CustomizationConfig.empty(),
            new ObjectMapperProvider().get(), null, new SchemaGeneratorProvider(Set.of()));
    private final ArgumentCaptor<AggregationRequestSpec> spec = ArgumentCaptor.forClass(AggregationRequestSpec.class);

    @BeforeEach
    void setUp() throws Exception {
        when(permissionHelper.getSearchUser()).thenReturn(searchUser);
        when(scriptingApiService.executeAggregation(any(), any())).thenReturn(new TabularResponse(List.of(), List.of(),
                new Metadata(AbsoluteRange.create("2026-10-05T14:00:00.000Z", "2026-10-05T14:05:00.000Z"))));
    }

    @Test
    void aggregatesTheLastSecondsByDefault() throws Exception {
        tool.apply(permissionHelper, Map.of("groupings", GROUPINGS, "metrics", METRICS));

        verify(scriptingApiService).executeAggregation(spec.capture(), any());
        assertThat(spec.getValue().timerange()).isEqualTo(RelativeRange.create(3600));
    }

    @Test
    void aggregatesAnAbsoluteTimeRange() throws Exception {
        tool.apply(permissionHelper, Map.of(
                "groupings", GROUPINGS,
                "metrics", METRICS,
                "from", "2026-10-05T14:00:00.000Z",
                "to", "2026-10-05T16:05:00+02:00",
                "range_seconds", 60));

        verify(scriptingApiService).executeAggregation(spec.capture(), any());
        assertThat(spec.getValue().timerange())
                .isEqualTo(AbsoluteRange.create("2026-10-05T14:00:00.000Z", "2026-10-05T14:05:00.000Z"));
    }

    @Test
    void acceptsTimestampsWithoutFractionalSeconds() throws Exception {
        tool.apply(permissionHelper, Map.of("groupings", GROUPINGS, "metrics", METRICS,
                "from", "2026-10-05T14:00:00Z", "to", "2026-10-05T14:05:00Z"));

        verify(scriptingApiService).executeAggregation(spec.capture(), any());
        assertThat(spec.getValue().timerange())
                .isEqualTo(AbsoluteRange.create("2026-10-05T14:00:00.000Z", "2026-10-05T14:05:00.000Z"));
    }

    @Test
    void needsBothEndsOfAnAbsoluteTimeRange() {
        assertThatThrownBy(() -> tool.apply(permissionHelper,
                Map.of("groupings", GROUPINGS, "metrics", METRICS, "from", "2026-10-05T14:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both from and to");
        verifyNoInteractions(scriptingApiService);
    }

    @Test
    void rejectsATimestampThatIsNotIso8601() {
        assertThatThrownBy(() -> tool.apply(permissionHelper,
                Map.of("groupings", GROUPINGS, "metrics", METRICS, "from", "yesterday", "to", "2026-10-05T14:05:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must be an ISO 8601 timestamp, got <yesterday>");
    }

    @Test
    void describesTheTimeRangeParameters() {
        assertThat(tool.inputSchema()).extractingByKey("properties")
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsKeys("from", "to", "range_seconds");
    }
}
