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
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.utilities.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;

import static org.graylog2.shared.utilities.StringUtils.f;

public class TimeBasedSizeOptimizingStrategy extends AbstractRotationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(TimeBasedSizeOptimizingStrategy.class);
    public static final String NAME = "time-size-optimizing";

    private final Indices indices;
    private final JobSchedulerClock clock;
    private final org.joda.time.Period rotationPeriod;

    private final Size maxIndexSize;
    private final Size minIndexSize;

    @Inject
    public TimeBasedSizeOptimizingStrategy(Indices indices,
                                           NodeId nodeId,
                                           AuditEventSender auditEventSender,
                                           ElasticsearchConfiguration elasticsearchConfiguration,
                                           JobSchedulerClock clock) {
        super(auditEventSender, nodeId, elasticsearchConfiguration);
        this.indices = indices;
        this.clock = clock;
        this.rotationPeriod = elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod();
        this.maxIndexSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMaxSize();
        this.minIndexSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMinSize();
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return TimeBasedSizeOptimizingStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return TimeBasedSizeOptimizingStrategyConfig.builder().build();
    }

    @Override
    @Nonnull
    protected Result shouldRotate(final String index, IndexSet indexSet) {
        final DateTime creationDate = indices.indexCreationDate(index).orElseThrow(()-> new IllegalStateException("No index creation date"));
        final Long sizeInBytes = indices.getStoreSizeInBytes(index).orElseThrow(() -> new IllegalStateException("No index size"));

        Period leeWay;
        if (indexSet.getConfig().rotationStrategy() instanceof TimeBasedSizeOptimizingStrategyConfig rotationConfig) {
            leeWay = rotationConfig.indexLifetimeHard().minus(rotationConfig.indexLifetimeSoft());
        } else {
            throw new IllegalStateException(f("Unsupported RotationStrategyConfig type <%s>", indexSet.getConfig().rotationStrategy()));
        }

        if (indices.numberOfMessages(index) == 0) {
            return createResult(false, "Index is empty");
        }

        if (indexExceedsSizeLimit(sizeInBytes)) {
            return createResult(true,
                    f("Index size <%s> exceeds MAX_INDEX_SIZE <%s>",
                            StringUtils.humanReadableByteCount(sizeInBytes), maxIndexSize));
        }
        if (indexExceedsLeeWay(creationDate, leeWay)) {
            return createResult(true,
                    f("Index creation date <%s> exceeds optimization leeway <%s>",
                            creationDate, leeWay));
        }

        if (indexIsOldEnough(creationDate) && !indexSubceedsSizeLimit(sizeInBytes)) {
            return createResult(true,
                    f("Index is old enough (%s) and has a reasonable size (%s) for rotation",
                            creationDate, StringUtils.humanReadableByteCount(sizeInBytes)));
        }

        return createResult(false, "No reason to rotate found");
    }

    private boolean indexExceedsLeeWay(DateTime creationDate, Period leeWay) {
        final Seconds leewaySeconds = Seconds.seconds(leeWay.toStandardSeconds().getSeconds());
        return timePassedIsBeyondLimit(creationDate, rotationPeriod.plus(leewaySeconds));
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

    private boolean indexExceedsSizeLimit(long size) {
        return size > maxIndexSize.toBytes();
    }

    private boolean indexSubceedsSizeLimit(long size) {
        return size < minIndexSize.toBytes();
    }

    @Override
    public String getStrategyName() {
        return NAME;
    }
}
