package org.graylog2.datatier;

import com.github.joschi.jadconfig.util.Size;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.IndexRotator;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;

import static org.graylog2.indexer.rotation.IndexRotator.createResult;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.shared.utilities.StringUtils.humanReadableByteCount;

public class DataTierRotation {

    private static final Logger LOG = LoggerFactory.getLogger(DataTierRotation.class);

    private final JobSchedulerClock clock;
    private final org.joda.time.Period rotationPeriod;

    private final Indices indices;

    private final IndexRotator indexRotator;

    private final Size maxShardSize;
    private final Size minShardSize;

    @Inject
    public DataTierRotation(Indices indices,
                            ElasticsearchConfiguration elasticsearchConfiguration,
                            IndexRotator indexRotator,
                            JobSchedulerClock clock) {

        this.indices = indices;
        this.indexRotator = indexRotator;
        this.clock = clock;
        this.rotationPeriod = elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod();
        this.maxShardSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMaxShardSize();
        this.minShardSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize();
    }

    public void rotate(IndexSet indexSet) {
        indexRotator.rotate(indexSet,this::shouldRotate);
    }

    @Nonnull
    protected IndexRotator.Result shouldRotate(final String index, IndexSet indexSet) {
        final DateTime creationDate = indices.indexCreationDate(index).orElseThrow(()-> new IllegalStateException("No index creation date"));
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


}
