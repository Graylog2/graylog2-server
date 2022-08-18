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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

/**
 * Migration creating the default stream if it doesn't exist.
 */
public class V20220818184438_RenameAllMessagesStreamMigration extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20220818184438_RenameAllMessagesStreamMigration.class);
    private final StreamService streamService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20220818184438_RenameAllMessagesStreamMigration(StreamService streamService, ClusterConfigService clusterConfigService) {
        this.streamService = streamService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-08-18T18:44:38Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }
        try {
            final Stream defaultStream = streamService.load(Stream.DEFAULT_STREAM_ID);
            defaultStream.setTitle("Default Stream");
            defaultStream.setDescription("Contains messages that are not explicitly routed to other streams");
            streamService.save(defaultStream);
        } catch (NotFoundException | ValidationException e) {
            LOG.error("Could not rename default stream", e);
        }
        clusterConfigService.write(MigrationCompleted.create());
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonCreator
        public static MigrationCompleted create() {
            return new AutoValue_V20220818184438_RenameAllMessagesStreamMigration_MigrationCompleted();
        }
    }
}
