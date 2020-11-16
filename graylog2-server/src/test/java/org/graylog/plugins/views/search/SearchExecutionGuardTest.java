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
package org.graylog.plugins.views.search;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.errors.MissingCapabilitiesException;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.ForbiddenException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.graylog.plugins.views.search.TestData.requirementsMap;
import static org.mockito.Mockito.mock;

public class SearchExecutionGuardTest {
    private SearchExecutionGuard sut;
    private Map<String, PluginMetaData> providedCapabilities;

    @Before
    public void setUp() throws Exception {
        GuiceInjectorHolder.createInjector(Collections.emptyList());

        providedCapabilities = new HashMap<>();
        providedCapabilities.put("my only capability", mock(PluginMetaData.class));

        sut = new SearchExecutionGuard(providedCapabilities);
    }

    @Test
    public void failsForNonPermittedStreams() {
        final Search search = searchWithStreamIds("ok", "not-ok");

        assertThatExceptionOfType(MissingStreamPermissionException.class)
                .isThrownBy(() -> sut.check(search, id -> id.equals("ok")))
                .satisfies(ex -> assertThat(ex.streamsWithMissingPermissions()).contains("not-ok"));
    }

    @Test
    public void succeedsIfAllStreamsArePermitted() {
        final Search search = searchWithStreamIds("ok", "ok-too", "this is fine...");

        assertSucceeds(search, id -> true);
    }

    @Test
    public void allowsSearchesWithNoStreams() {
        final Search search = searchWithStreamIds();

        assertSucceeds(search, id -> true);
    }

    @Test
    public void failsForMissingCapabilities() {
        final Search search = searchWithCapabilityRequirements("awesomeness");

        assertThatExceptionOfType(MissingCapabilitiesException.class)
                .isThrownBy(() -> sut.check(search, id -> true))
                .satisfies(ex -> assertThat(ex.getMissingRequirements()).containsOnlyKeys("awesomeness"));
    }

    @Test
    public void succeedsIfCapabilityRequirementsFulfilled() {
        final String onlyRequirement = new ArrayList<>(providedCapabilities.keySet()).get(0);
        final Search search = searchWithCapabilityRequirements(onlyRequirement);

        assertSucceeds(search, id -> true);
    }

    private void assertSucceeds(Search search, Predicate<String> isStreamIdPermitted) {
        assertThatCode(() -> sut.check(search, isStreamIdPermitted)).doesNotThrowAnyException();
    }

    private Search searchWithCapabilityRequirements(String... requirementNames) {
        final Search search = searchWithStreamIds("streamId");

        final Map<String, PluginMetadataSummary> requirements = requirementsMap(requirementNames);

        return search.toBuilder().requires(requirements).build();
    }

    private Search searchWithStreamIds(String... streamIds) {
        final StreamFilter[] filters = Arrays.stream(streamIds).map(StreamFilter::ofId).toArray(StreamFilter[]::new);

        final Query query = Query.builder()
                .id("")
                .timerange(mock(TimeRange.class))
                .query(new BackendQuery.Fallback())
                .filter(OrFilter.or(filters))
                .build();
        return Search.Builder.create().id("searchId").queries(ImmutableSet.of(query)).build();
    }
}
