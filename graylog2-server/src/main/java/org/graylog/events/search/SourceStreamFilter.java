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

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controls source stream filtering for event searches based on user permissions.
 * <p>
 * Use {@link #allAllowed()} when the user has blanket {@code streams:read} permission.
 * Use {@link #allowList(Set)} to restrict results to events matching the given source streams.
 */
public record SourceStreamFilter(boolean isAllAllowed, Set<String> streamIds) {

    public static SourceStreamFilter allAllowed() {
        return new SourceStreamFilter(true, Set.of());
    }

    public static SourceStreamFilter allowList(Set<String> streamIds) {
        return new SourceStreamFilter(false, streamIds);
    }

    /**
     * Creates a {@link SourceStreamFilter} based on the given subject's stream permissions.
     * Returns {@link #allAllowed()} if the subject has blanket {@code streams:read} permission,
     * otherwise returns an allow-list of streams the subject is permitted to read.
     */
    public static SourceStreamFilter forSubject(Subject subject, StreamService streamService) {
        if (subject.isPermitted(RestPermissions.STREAMS_READ)) {
            return allAllowed();
        }
        try (var stream = streamService.streamAllIds()) {
            return allowList(stream
                    .filter(id -> subject.isPermitted(String.join(":", RestPermissions.STREAMS_READ, id)))
                    .collect(Collectors.toSet()));
        }
    }
}
