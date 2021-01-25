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
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENT_STREAM_IDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermittedStreamsTest {

    private StreamService streamService;
    private PermittedStreams sut;

    @Before
    public void setUp() throws Exception {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        streamService = mock(StreamService.class);
        sut = new PermittedStreams(streamService);
    }

    @Test
    public void findsStreams() {
        stubStreams("oans", "zwoa", "gsuffa");

        ImmutableSet<String> result = sut.load(id -> true);

        assertThat(result).containsExactlyInAnyOrder("oans", "zwoa", "gsuffa");
    }

    @Test
    public void filtersOutNonPermittedStreams() {
        stubStreams("oans", "zwoa", "gsuffa");

        ImmutableSet<String> result = sut.load(id -> id.equals("gsuffa"));

        assertThat(result).containsExactly("gsuffa");
    }

    @Test
    public void returnsEmptyListIfNoStreamsFound() {
        stubStreams("oans", "zwoa", "gsuffa");

        ImmutableSet<String> result = sut.load(id -> false);

        assertThat(result).isEmpty();
    }

    @Test
    public void filtersDefaultStreams() {
        List<String> streamIds = new ArrayList<>(DEFAULT_EVENT_STREAM_IDS);
        streamIds.add("i'm ok");

        stubStreams(streamIds.toArray(new String[]{}));

        ImmutableSet<String> result = sut.load(id -> true);

        assertThat(result).containsExactly("i'm ok");
    }

    private void stubStreams(String... streamIds) {
        List<Stream> streams = streamsWithIds(streamIds);
        when(streamService.loadAll()).thenReturn(streams);
    }

    private List<Stream> streamsWithIds(String... ids) {
        return Arrays.stream(ids).map(this::streamWithId).collect(toList());
    }

    private Stream streamWithId(String id) {
        Stream s = mock(Stream.class);
        when(s.getId()).thenReturn(id);
        return s;
    }
}
