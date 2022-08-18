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
package org.graylog.plugins.views.search.permissions;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.rest.FieldTypesResource;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserStreams {
    private final StreamPermissions streamPermissions;
    private final PermittedStreams permittedStreams;

    private static final Logger LOG = LoggerFactory.getLogger(FieldTypesResource.class);

    public UserStreams(StreamPermissions streamPermissions, PermittedStreams permittedStreams) {
        this.streamPermissions = streamPermissions;
        this.permittedStreams = permittedStreams;
    }

    public ImmutableSet<String> loadAll() {
        return permittedStreams.load(streamPermissions);
    }

    /**
     * If any stream IDs are provided, they will be filtered out by read permission. If none are given, we'll load
     * all available streams for the current SearchUser
     *
     * @param requestedStreams requested stream IDs that should be used in the search
     * @return Filtered and readable stream IDs.
     */
    public ImmutableSet<String> readableOrAllIfEmpty(@Nullable final Set<String> requestedStreams) {
        if (requestedStreams == null || requestedStreams.isEmpty()) {
            return loadAll();
        } else {

            final Set<String> notPermittedStreams = requestedStreams.stream()
                    .filter(s -> !streamPermissions.canReadStream(s))
                    .collect(Collectors.toSet());

            if (!notPermittedStreams.isEmpty()) {
                LOG.info("Not authorized to access resource id <{}>. User is missing permission <{}:{}>",
                        notPermittedStreams, RestPermissions.STREAMS_READ, notPermittedStreams);
                throw new MissingStreamPermissionException("Not authorized to access streams.",
                        notPermittedStreams);
            }

            return ImmutableSet.copyOf(requestedStreams);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ImmutableSet<String> readableOrAllIfEmpty(Optional<Set<String>> requestedStreams) {
        return readableOrAllIfEmpty(requestedStreams.orElse(null));
    }
}
