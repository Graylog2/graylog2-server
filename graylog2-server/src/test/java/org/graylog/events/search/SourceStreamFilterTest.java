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
package org.graylog.events.search;

import org.apache.shiro.subject.Subject;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceStreamFilterTest {

    @Mock
    private Subject subject;

    @Mock
    private StreamService streamService;

    @Test
    void allAllowedReturnsFilterWithNoRestrictions() {
        final var filter = SourceStreamFilter.allAllowed();

        assertThat(filter.isAllAllowed()).isTrue();
        assertThat(filter.streamIds()).isEmpty();
    }

    @Test
    void allowListReturnsFilterWithSpecificStreams() {
        final var filter = SourceStreamFilter.allowList(Set.of("stream-a", "stream-b"));

        assertThat(filter.isAllAllowed()).isFalse();
        assertThat(filter.streamIds()).containsExactlyInAnyOrder("stream-a", "stream-b");
    }

    @Test
    void forSubjectReturnsAllAllowedWhenSubjectHasBlanketPermission() {
        when(subject.isPermitted(RestPermissions.STREAMS_READ)).thenReturn(true);

        final var filter = SourceStreamFilter.forSubject(subject, streamService);

        assertThat(filter.isAllAllowed()).isTrue();
        assertThat(filter.streamIds()).isEmpty();
    }

    @Test
    void forSubjectReturnsAllowListOfPermittedStreams() {
        when(subject.isPermitted(RestPermissions.STREAMS_READ)).thenReturn(false);
        when(subject.isPermitted("streams:read:stream-a")).thenReturn(true);
        when(subject.isPermitted("streams:read:stream-b")).thenReturn(false);
        when(subject.isPermitted("streams:read:stream-c")).thenReturn(true);
        when(streamService.streamAllIds()).thenReturn(Stream.of("stream-a", "stream-b", "stream-c"));

        final var filter = SourceStreamFilter.forSubject(subject, streamService);

        assertThat(filter.isAllAllowed()).isFalse();
        assertThat(filter.streamIds()).containsExactlyInAnyOrder("stream-a", "stream-c");
    }

    @Test
    void forSubjectReturnsEmptyAllowListWhenNoStreamsPermitted() {
        when(subject.isPermitted(RestPermissions.STREAMS_READ)).thenReturn(false);
        when(subject.isPermitted("streams:read:stream-a")).thenReturn(false);
        when(streamService.streamAllIds()).thenReturn(Stream.of("stream-a"));

        final var filter = SourceStreamFilter.forSubject(subject, streamService);

        assertThat(filter.isAllAllowed()).isFalse();
        assertThat(filter.streamIds()).isEmpty();
    }
}