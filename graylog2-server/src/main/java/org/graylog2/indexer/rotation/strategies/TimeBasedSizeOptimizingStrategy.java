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
package org.graylog2.indexer.rotation.strategies;

import com.github.joschi.jadconfig.util.Size;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.shared.utilities.StringUtils.humanReadableByteCount;

public class TimeBasedSizeOptimizingStrategy extends AbstractRotationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(TimeBasedSizeOptimizingStrategy.class);
    public static final String NAME = "time-size-optimizing";

    private final JobSchedulerClock clock;
    private final org.joda.time.Period rotationPeriod;

    private final Size maxShardSize;
    private final Size minShardSize;

    @Inject
    public TimeBasedSizeOptimizingStrategy(Indices indices,
                                           NodeId nodeId,
                                           AuditEventSender auditEventSender,
                                           ElasticsearchConfiguration elasticsearchConfiguration,
                                           JobSchedulerClock clock) {
        super(auditEventSender, nodeId, elasticsearchConfiguration, indices);
        this.clock = clock;
        this.rotationPeriod = elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod();
        this.maxShardSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMaxShardSize();
        this.minShardSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize();
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return TimeBasedSizeOptimizingStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return TimeBasedSizeOptimizingStrategyConfig.builder()
                .indexLifetimeMin(elasticsearchConfiguration.getTimeSizeOptimizingRetentionMinLifeTime())
                .indexLifetimeMax(elasticsearchConfiguration.getTimeSizeOptimizingRetentionMaxLifeTime())
                .build();
    }

    @Override
    @Nonnull
    protected Result shouldRotate(final String index, IndexSet indexSet) {
        final DateTime creationDate = indices.indexCreationDate(index).orElseThrow(() -> new IllegalStateException("No index creation date"));
        final Long sizeInBytes = indices.getStoreSizeInBytes(index).orElseThrow(() -> new IllegalStateException("No index size"));

        if (!(indexSet.getConfig().rotationStrategy() instanceof TimeBasedSizeOptimizingStrategyConfig config)) {
            throw new IllegalStateException(f("Unsupported RotationStrategyConfig type <%s>", indexSet.getConfig().rotationStrategy()));
        }

        if (indices.numberOfMessages(index) == 0) {
            return createResult(false, "Index is empty");
        }

        final int shards = indexSet.getConfig().shards();

        final long maxIndexSize = maxShardSize.toBytes() * shards;
        if (sizeInBytes > maxIndexSize) {
            return createResult(true,
                    f("Index size <%s> exceeds maximum size <%s>",
                            humanReadableByteCount(sizeInBytes), humanReadableByteCount(maxIndexSize)));
        }

        // If no retention is selected, we have an "indefinite" optimization leeway
        if (!(indexSet.getConfig().retentionStrategy() instanceof NoopRetentionStrategyConfig)) {
            Period leeWay = config.indexLifetimeMax().minus(config.indexLifetimeMin());
            if (indexExceedsLeeWay(creationDate, leeWay)) {
                return createResult(true,
                        f("Index creation date <%s> exceeds optimization leeway <%s>",
                                creationDate, leeWay));
            }
        }

        final long minIndexSize = minShardSize.toBytes() * shards;
        if (indexIsOldEnough(creationDate) && sizeInBytes >= minIndexSize) {
            return createResult(true,
                    f("Index creation date <%s> has passed rotation period <%s> and has a reasonable size <%s> for rotation",
                            creationDate, rotationPeriod, humanReadableByteCount(minIndexSize)));
        }

        return createResult(false, "No reason to rotate found");
    }

    private boolean indexExceedsLeeWay(DateTime creationDate, Period leeWay) {
        return timePassedIsBeyondLimit(creationDate, leeWay);
    }

    private boolean indexIsOldEnough(DateTime creationDate) {
        return timePassedIsBeyondLimit(creationDate, rotationPeriod);
    }

    private boolean timePassedIsBeyondLimit(DateTime date, Period limit) {
        final Instant now = clock.instantNow();
        final Duration timePassed = Duration.between(Instant.ofEpochMilli(date.getMillis()), now);
        final Duration limitAsDuration = Duration.ofSeconds(limit.toStandardSeconds().getSeconds());

        return timePassed.compareTo(limitAsDuration) >= 0;
    }

    @Override
    public String getStrategyName() {
        return NAME;
    }
}
