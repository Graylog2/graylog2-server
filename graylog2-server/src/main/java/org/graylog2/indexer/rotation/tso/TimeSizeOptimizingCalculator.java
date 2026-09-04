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
package org.graylog2.indexer.rotation.tso;

import com.github.joschi.jadconfig.util.Size;
import com.google.common.base.Suppliers;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.system.stats.elasticsearch.NodeOSInfo;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.shared.utilities.StringUtils.humanReadableByteCount;

@Singleton
public class TimeSizeOptimizingCalculator {

    public static final long FALLBACK_SHARD_SIZE_BYTES = Size.gigabytes(20).toBytes();
    private static final long MIN_SHARD_SIZE_BYTES = Size.gigabytes(5).toBytes();
    // The data_hot role is only used for Elasticsearch nodes
    private static final Set<String> DATA_NODE_ROLES = Set.of("data", "data_hot");
    private final Indices indices;
    private final JobSchedulerClock clock;
    private final ElasticsearchConfiguration opensearchConfig;
    private final ClusterAdapter clusterAdapter;
    private final Supplier<Optional<Long>> dataNodeMinOsMemorySupplier =
            Suppliers.memoizeWithExpiration(this::computeDataNodeMinOsMemorySizeInBytes, 1, TimeUnit.MINUTES);

    @Inject
    public TimeSizeOptimizingCalculator(Indices indices,
                                        JobSchedulerClock clock,
                                        ElasticsearchConfiguration opensearchConfig,
                                        ClusterAdapter clusterAdapter) {
        this.indices = indices;
        this.clock = clock;
        this.opensearchConfig = opensearchConfig;
        this.clusterAdapter = clusterAdapter;

    }

    public IndexRotator.Result calculate(final String index, IndexLifetimeConfig indexLifetimeConfig, IndexSetConfig config) {
        final DateTime creationDate = indices.indexCreationDate(index).orElseThrow(() -> new IllegalStateException("No index creation date"));
        final Long sizeInBytes = indices.getStoreSizeInBytes(index).orElseThrow(() -> new IllegalStateException("No index size"));

        if (indices.numberOfMessages(index) == 0) {
            return createResult(false, "Index is empty");
        }

        // If no retention is selected, we have an "indefinite" optimization leeway
        if (config.dataTieringConfig() != null || !(config.retentionStrategyConfig() instanceof NoopRetentionStrategyConfig)) {
            Period leeWay = indexLifetimeConfig.indexLifetimeMax().minus(indexLifetimeConfig.indexLifetimeMin());
            if (indexExceedsLeeWay(creationDate, leeWay)) {
                return createResult(true,
                        f("Index creation date <%s> exceeds optimization leeway <%s>", creationDate, leeWay));
            }
        }

        if (useDynamicShardSizing()) {
            return calculateResultWithDynamicShardSize(config, sizeInBytes);
        } else {
            return calculateResultUsingMinMaxShardSize(config, sizeInBytes, creationDate);
        }
    }

    private IndexRotator.Result calculateResultWithDynamicShardSize(IndexSetConfig config, Long sizeInBytes) {
        Optional<Long> dataNodeMinOSMemorySizeInBytes = dataNodeMinOSMemorySizeInBytes();
        String additionalResultText;
        long maxShardSize;
        if (dataNodeMinOSMemorySizeInBytes.isPresent()) {
            long dynamicShardSize = (long) (dataNodeMinOSMemorySizeInBytes.get() * opensearchConfig.getTimeSizeOptimizingRotationOSMemoryFactor());
            if (dynamicShardSize < MIN_SHARD_SIZE_BYTES) {
                maxShardSize = MIN_SHARD_SIZE_BYTES;
                additionalResultText = f("Dynamic shard size <%s * %s ≈ %s> is less than minimum. Using minimum shard size <%s>.",
                        humanReadableByteCount(dataNodeMinOSMemorySizeInBytes.get()), opensearchConfig.getTimeSizeOptimizingRotationOSMemoryFactor(),
                        humanReadableByteCount(dynamicShardSize), humanReadableByteCount(MIN_SHARD_SIZE_BYTES));
            } else {
                maxShardSize = dynamicShardSize;
                additionalResultText = f("Calculated dynamic shard size <%s * %s ≈ %s>.", humanReadableByteCount(dataNodeMinOSMemorySizeInBytes.get()),
                        opensearchConfig.getTimeSizeOptimizingRotationOSMemoryFactor(), humanReadableByteCount(dynamicShardSize));
            }
        } else {
            maxShardSize = FALLBACK_SHARD_SIZE_BYTES;
            additionalResultText = f("Could not calculate dynamic shard size. Fallback to shard size <%s>.",
                    humanReadableByteCount(FALLBACK_SHARD_SIZE_BYTES));
        }

        long maxIndexSize = maxShardSize * config.shards();
        if (sizeInBytes > maxIndexSize) {
            return createResult(true, f("Index size <%s> exceeds maximum size <%s> (shards: %s). %s",
                    humanReadableByteCount(sizeInBytes), humanReadableByteCount(maxIndexSize), config.shards(), additionalResultText));
        }
        return notRotateResult();
    }

    private IndexRotator.Result calculateResultUsingMinMaxShardSize(IndexSetConfig config, Long sizeInBytes, DateTime creationDate) {
        long maxIndexSize = opensearchConfig.getTimeSizeOptimizingRotationMaxShardSize().toBytes() * config.shards();
        if (sizeInBytes > maxIndexSize) {
            return createResult(true,
                    f("Index size <%s> exceeds maximum size <%s>",
                            humanReadableByteCount(sizeInBytes), humanReadableByteCount(maxIndexSize)));
        }
        final long minIndexSize = opensearchConfig.getTimeSizeOptimizingRotationMinShardSize().toBytes() * config.shards();
        if (indexIsOldEnough(creationDate) && sizeInBytes >= minIndexSize) {
            return createResult(true,
                    f("Index creation date <%s> has passed rotation period <%s> and has a reasonable size <%s> for rotation",
                            creationDate, opensearchConfig.getTimeSizeOptimizingRotationPeriod(), humanReadableByteCount(minIndexSize)));
        }
        return notRotateResult();
    }

    private IndexRotator.Result notRotateResult() {
        return createResult(false, "No reason to rotate found");
    }

    private boolean useDynamicShardSizing() {
        return opensearchConfig.getTimeSizeOptimizingRotationMaxShardSize() == null || opensearchConfig.getTimeSizeOptimizingRotationMinShardSize() == null;
    }

    private Optional<Long> dataNodeMinOSMemorySizeInBytes() {
        return dataNodeMinOsMemorySupplier.get();
    }

    private Optional<Long> computeDataNodeMinOsMemorySizeInBytes() {
        return clusterAdapter.nodesHostInfo().values().stream()
                .filter(node -> DATA_NODE_ROLES.stream().anyMatch(r -> node.roles().contains(r)))
                .map(NodeOSInfo::memoryTotalInBytes)
                .min(Long::compareTo);
    }

    private boolean indexIsOldEnough(DateTime creationDate) {
        return timePassedIsBeyondLimit(creationDate, opensearchConfig.getTimeSizeOptimizingRotationPeriod());
    }

    private boolean timePassedIsBeyondLimit(DateTime date, Period limit) {
        final Instant now = clock.instantNow();
        final Duration timePassed = Duration.between(Instant.ofEpochMilli(date.getMillis()), now);
        final Duration limitAsDuration = Duration.ofSeconds(limit.toStandardSeconds().getSeconds());

        return timePassed.compareTo(limitAsDuration) >= 0;
    }

    private IndexRotator.Result createResult(boolean shouldRotate, String message) {
        return IndexRotator.createResult(shouldRotate, message, this.getClass().getCanonicalName());
    }

    private boolean indexExceedsLeeWay(DateTime creationDate, Period leeWay) {
        return timePassedIsBeyondLimit(creationDate, leeWay);
    }
}
