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

import jakarta.inject.Inject;
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

import java.time.ZonedDateTime;

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

        final Stream stream = StreamImpl.builder()
                .id(Stream.DEFAULT_STREAM_ID)
                .title("Default Stream")
                .description("Contains messages that are not explicitly routed to other streams")
                .disabled(false)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .creatorUserId("local:admin")
                .matchingType(StreamImpl.MatchingType.DEFAULT)
                .removeMatchesFromDefaultStream(false)
                .isDefault(true)
                .indexSetId(indexSet.getConfig().id())
                .build();

        try {
            streamService.save(stream);
            LOG.info("Successfully created default stream: {}", stream.getTitle());
        } catch (ValidationException e) {
            LOG.error("Couldn't create default stream! This is a bug!");
        }
    }
}
