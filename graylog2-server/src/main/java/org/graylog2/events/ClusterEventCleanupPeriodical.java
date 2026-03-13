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
import jakarta.inject.Named;
import org.graylog2.database.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class ClusterEventCleanupPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventCleanupPeriodical.class);
    private static final String COLLECTION_NAME = ClusterEventPeriodical.COLLECTION_NAME;
    private static final long MIN_PERIOD_SECONDS = 3600;

    private final MongoCollection<ClusterEvent> collection;
    private final Duration maxEventAge;
    private final Clock clock;

    @Inject
    public ClusterEventCleanupPeriodical(MongoCollections mongoCollections, @Named("max_event_age") Duration maxEventAge) {
        this(mongoCollections, maxEventAge, Clock.systemUTC());
    }

    @VisibleForTesting
    ClusterEventCleanupPeriodical(MongoCollections mongoCollections, Duration maxEventAge, Clock clock) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, ClusterEvent.class)
                .withWriteConcern(WriteConcern.JOURNALED);
        this.maxEventAge = maxEventAge;
        this.clock = clock;
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
        return Ints.saturatedCast(Math.max(MIN_PERIOD_SECONDS, maxEventAge.toSeconds()));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        try {
            LOG.debug("Removing stale events from MongoDB collection \"{}\"", COLLECTION_NAME);

            final long timestamp = Instant.now(clock).minus(maxEventAge).toEpochMilli();
            final var deleted = collection.deleteMany(Filters.lt("timestamp", timestamp)).getDeletedCount();

            LOG.debug("Removed {} stale events from \"{}\"", deleted, COLLECTION_NAME);
        } catch (Exception e) {
            LOG.warn("Error while removing stale cluster events from MongoDB", e);
        }
    }
}
