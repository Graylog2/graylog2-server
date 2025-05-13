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
package org.graylog2.events;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ClusterEventCleanupPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventCleanupPeriodical.class);
    private static final String COLLECTION_NAME = ClusterEventPeriodical.COLLECTION_NAME;

    @VisibleForTesting
    static final long DEFAULT_MAX_EVENT_AGE = TimeUnit.DAYS.toMillis(1L);

    private final MongoCollection<ClusterEvent> collection;

    @Inject
    public ClusterEventCleanupPeriodical(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, ClusterEvent.class)
                .withWriteConcern(WriteConcern.JOURNALED);
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return Ints.saturatedCast(TimeUnit.DAYS.toSeconds(1L));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        try {
            LOG.debug("Removing stale events from MongoDB collection \"{}\"", COLLECTION_NAME);

            final long timestamp = DateTime.now(DateTimeZone.UTC).getMillis() - DEFAULT_MAX_EVENT_AGE;
            final var deleted = collection.deleteMany(Filters.lt("timestamp", timestamp)).getDeletedCount();

            LOG.debug("Removed {} stale events from \"{}\"", deleted, COLLECTION_NAME);
        } catch (Exception e) {
            LOG.warn("Error while removing stale cluster events from MongoDB", e);
        }
    }
}
