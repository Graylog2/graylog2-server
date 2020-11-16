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
package org.graylog2.migrations;

import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Migration creating the default stream if it doesn't exist.
 */
public class V20161116172200_CreateDefaultStreamMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161116172200_CreateDefaultStreamMigration.class);

    private final StreamService streamService;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public V20161116172200_CreateDefaultStreamMigration(StreamService streamService,
                                                        IndexSetRegistry indexSetRegistry) {
        this.streamService = streamService;
        this.indexSetRegistry = indexSetRegistry;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-11-16T17:22:00Z");
    }

    @Override
    public void upgrade() {
        try {
            streamService.load(Stream.DEFAULT_STREAM_ID);
        } catch (NotFoundException ignored) {
            createDefaultStream();
        }
    }

    private void createDefaultStream() {
        final IndexSet indexSet = indexSetRegistry.getDefault();

        final ObjectId id = new ObjectId(Stream.DEFAULT_STREAM_ID);
        final Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put(StreamImpl.FIELD_TITLE, "All messages")
                .put(StreamImpl.FIELD_DESCRIPTION, "Stream containing all messages")
                .put(StreamImpl.FIELD_DISABLED, false)
                .put(StreamImpl.FIELD_CREATED_AT, DateTime.now(DateTimeZone.UTC))
                .put(StreamImpl.FIELD_CREATOR_USER_ID, "local:admin")
                .put(StreamImpl.FIELD_MATCHING_TYPE, StreamImpl.MatchingType.DEFAULT.name())
                .put(StreamImpl.FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM, false)
                .put(StreamImpl.FIELD_DEFAULT_STREAM, true)
                .put(StreamImpl.FIELD_INDEX_SET_ID, indexSet.getConfig().id())
                .build();
        final Stream stream = new StreamImpl(id, fields, Collections.emptyList(), Collections.emptySet(), indexSet);

        try {
            streamService.save(stream);
            LOG.info("Successfully created default stream: {}", stream.getTitle());
        } catch (ValidationException e) {
            LOG.error("Couldn't create default stream! This is a bug!");
        }
    }
}
