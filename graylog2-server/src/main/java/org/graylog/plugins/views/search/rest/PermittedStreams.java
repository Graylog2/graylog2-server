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
import org.graylog.plugins.views.search.permissions.StreamPermissions;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.plugin.streams.Stream.NON_MESSAGE_STREAM_IDS;

public class PermittedStreams {
    private final Supplier<Stream<String>> allStreamsProvider;


    public PermittedStreams(Supplier<Stream<String>> allStreamsProvider) {
        this.allStreamsProvider = allStreamsProvider;
    }

    @Inject
    public PermittedStreams(StreamService streamService) {
        this(() -> streamService.loadAll().stream().map(org.graylog2.plugin.streams.Stream::getId));
    }

    public ImmutableSet<String> load(StreamPermissions streamPermissions) {
        return allStreamsProvider.get()
                // Unless explicitly queried, exclude event and failure indices by default
                // Having these indices in every search, makes sorting almost impossible
                // because it triggers https://github.com/Graylog2/graylog2-server/issues/6378
                // TODO: this filter could be removed, once we implement https://github.com/Graylog2/graylog2-server/issues/6490
                .filter(id -> !NON_MESSAGE_STREAM_IDS.contains(id))
                .filter(streamPermissions::canReadStream)
                .collect(ImmutableSet.toImmutableSet());
    }
}
