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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.NON_MESSAGE_STREAM_IDS;

public class PermittedStreamsTest {

    @Test
    public void findsStreams() {
        final PermittedStreams sut = new PermittedStreams(() -> java.util.stream.Stream.of("oans", "zwoa", "gsuffa"), (categories) -> Stream.of());
        ImmutableSet<String> result = sut.loadAllMessageStreams(id -> true);
        assertThat(result).containsExactlyInAnyOrder("oans", "zwoa", "gsuffa");
    }

    @Test
    public void filtersOutNonPermittedStreams() {
        final PermittedStreams sut = new PermittedStreams(() -> java.util.stream.Stream.of("oans", "zwoa", "gsuffa"), (categories) -> Stream.of());
        ImmutableSet<String> result = sut.loadAllMessageStreams(id -> id.equals("gsuffa"));
        assertThat(result).containsExactly("gsuffa");
    }

    @Test
    public void returnsEmptyListIfNoStreamsFound() {
        final PermittedStreams sut = new PermittedStreams(() -> java.util.stream.Stream.of("oans", "zwoa", "gsuffa"), (categories) -> Stream.of());
        ImmutableSet<String> result = sut.loadAllMessageStreams(id -> false);
        assertThat(result).isEmpty();
    }

    @Test
    public void filtersDefaultStreams() {
        final PermittedStreams sut = new PermittedStreams(() -> Streams.concat(NON_MESSAGE_STREAM_IDS.stream(), java.util.stream.Stream.of("i'm ok")), (categories) -> Stream.of());
        ImmutableSet<String> result = sut.loadAllMessageStreams(id -> true);
        assertThat(result).containsExactly("i'm ok");
    }

    @Test
    public void findsStreamsFromCategories() {
        final PermittedStreams sut = new PermittedStreams(Stream::of, this::categoryMapping);
        ImmutableSet<String> result = sut.loadWithCategories(List.of("colors"), (streamId) -> true);
        assertThat(result).containsExactlyInAnyOrder("red", "yellow", "blue");
    }

    @Test
    public void findsStreamsFromMultipleCategories() {
        final PermittedStreams sut = new PermittedStreams(Stream::of, this::categoryMapping);
        ImmutableSet<String> result = sut.loadWithCategories(List.of("colors", "numbers"), (streamId) -> true);
        assertThat(result).containsExactlyInAnyOrder("red", "yellow", "blue", "one", "two", "three");
    }

    @Test
    public void findsStreamsFromCategoriesWithPermissions() {
        final PermittedStreams sut = new PermittedStreams(Stream::of, this::categoryMapping);
        ImmutableSet<String> result = sut.loadWithCategories(List.of("colors"), (streamId) -> streamId.equals("red") || streamId.equals("blue"));
        assertThat(result).containsExactlyInAnyOrder("red", "blue");
    }

    @Test
    public void returnsEmptyIfNoStreamCategoriesMatch() {
        final PermittedStreams sut = new PermittedStreams(Stream::of, this::categoryMapping);
        ImmutableSet<String> result = sut.loadWithCategories(List.of("invalid_category"), (streamId) -> true);
        assertThat(result).isEmpty();
    }

    @Test
    public void returnsEmptyIfPermissionsFilter() {
        final PermittedStreams sut = new PermittedStreams(Stream::of, this::categoryMapping);
        ImmutableSet<String> result = sut.loadWithCategories(List.of("colors", "numbers", "animals"), (streamId) -> false);
        assertThat(result).isEmpty();
    }

    private Stream<String> categoryMapping(Collection<String> categories) {
        Set<String> streams = new HashSet<>();
        if (categories.contains("colors")) {
            streams.addAll(List.of("red", "yellow", "blue"));
        }
        if (categories.contains("numbers")) {
            streams.addAll(List.of("one", "two", "three"));
        }
        if (categories.contains("animals")) {
            streams.addAll(List.of("cat", "dog", "fox"));
        }
        return streams.stream();
    }
}
