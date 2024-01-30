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
package org.graylog2.datatiering.rotation;

import com.github.joschi.jadconfig.util.Size;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.shared.utilities.StringUtils.humanReadableByteCount;

public class DataTierRotation {

    private final JobSchedulerClock clock;
    private final org.joda.time.Period rotationPeriod;

    private final Indices indices;

    private final IndexRotator indexRotator;

    private final Size maxShardSize;
    private final Size minShardSize;

    private final IndexLifetimeConfig indexLifetimeConfig;

    @AssistedInject
    public DataTierRotation(Indices indices,
                            ElasticsearchConfiguration elasticsearchConfiguration,
                            IndexRotator indexRotator,
                            JobSchedulerClock clock,
                            @Assisted IndexLifetimeConfig indexLifetimeConfig) {

        this.indices = indices;
        this.indexRotator = indexRotator;
        this.clock = clock;
        this.rotationPeriod = elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod();
        this.maxShardSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMaxShardSize();
        this.minShardSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize();
        this.indexLifetimeConfig = indexLifetimeConfig;
    }

    public void rotate(IndexSet indexSet) {
        indexRotator.rotate(indexSet, this::shouldRotate);
    }

    private IndexRotator.Result createResult(boolean shouldRotate, String message) {
        return IndexRotator.createResult(shouldRotate, message, this.getClass().getCanonicalName());
    }

    @Nonnull
    private IndexRotator.Result shouldRotate(final String index, IndexSet indexSet) {
        final DateTime creationDate = indices.indexCreationDate(index).orElseThrow(() -> new IllegalStateException("No index creation date"));
        final Long sizeInBytes = indices.getStoreSizeInBytes(index).orElseThrow(() -> new IllegalStateException("No index size"));

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
            Period leeWay = indexLifetimeConfig.indexLifetimeMax().minus(indexLifetimeConfig.indexLifetimeMin());
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

    public interface Factory {
        DataTierRotation create(IndexLifetimeConfig retentionConfig);

    }
}
