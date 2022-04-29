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
import com.google.common.collect.Streams;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.NON_MESSAGE_STREAM_IDS;

public class PermittedStreamsTest {

    @Test
    public void findsStreams() {
        final PermittedStreams sut = new PermittedStreams(() -> java.util.stream.Stream.of("oans", "zwoa", "gsuffa"));
        ImmutableSet<String> result = sut.load(id -> true);
        assertThat(result).containsExactlyInAnyOrder("oans", "zwoa", "gsuffa");
    }

    @Test
    public void filtersOutNonPermittedStreams() {
        final PermittedStreams sut = new PermittedStreams(() -> java.util.stream.Stream.of("oans", "zwoa", "gsuffa"));
        ImmutableSet<String> result = sut.load(id -> id.equals("gsuffa"));
        assertThat(result).containsExactly("gsuffa");
    }

    @Test
    public void returnsEmptyListIfNoStreamsFound() {
        final PermittedStreams sut = new PermittedStreams(() -> java.util.stream.Stream.of("oans", "zwoa", "gsuffa"));
        ImmutableSet<String> result = sut.load(id -> false);
        assertThat(result).isEmpty();
    }

    @Test
    public void filtersDefaultStreams() {
        final PermittedStreams sut = new PermittedStreams(() -> Streams.concat(NON_MESSAGE_STREAM_IDS.stream(), java.util.stream.Stream.of("i'm ok")));
        ImmutableSet<String> result = sut.load(id -> true);
        assertThat(result).containsExactly("i'm ok");
    }
}
