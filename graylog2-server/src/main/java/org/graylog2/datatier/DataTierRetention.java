package org.graylog2.datatier;

import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.datatier.tier.DataTier;
import org.graylog2.datatier.tier.DataTierType;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.periodical.IndexRetentionThread;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;

public class DataTierRetention {

    private static final Logger LOG = LoggerFactory.getLogger(DataTierRetention.class);

    private final Indices indices;
    private final JobSchedulerClock clock;
    private final ActivityWriter activityWriter;

    @Inject
    public DataTierRetention(Indices indices,
                             JobSchedulerClock clock,
                             ActivityWriter activityWriter) {
        this.indices = indices;
        this.clock = clock;
        this.activityWriter = activityWriter;
    }

    private static boolean hasCurrentWriteAlias(IndexSet indexSet, Map<String, Set<String>> deflectorIndices, String indexName) {
        return deflectorIndices.getOrDefault(indexName, Collections.emptySet()).contains(indexSet.getWriteIndexAlias());
    }

    public void retain(IndexSet indexSet) {
        List<DataTier> dataTiers = indexSet.getConfig().dataTiers();
        if (dataTiers == null) {
            return;
        }
        dataTiers.stream()
                .filter(tier -> DataTierType.HOT.equals(tier.getTier()))
                .findFirst()
                .ifPresent(tier -> retain(indexSet, tier));
    }

    private void retain(IndexSet indexSet, DataTier tier) {
        final Map<String, Set<String>> deflectorIndices = indexSet.getAllIndexAliases();

        // Account for DST and time zones in determining age
        final DateTime now = clock.nowUTC();
        final long cutoffSoft = now.minus(tier.indexLifetimeMin()).getMillis();
        final long cutoffHard = now.minus(tier.indexLifetimeMax()).getMillis();
        final int removeCount = (int) deflectorIndices.keySet()
                .stream()
                .filter(indexName -> !indices.isReopened(indexName))
                .filter(indexName -> !hasCurrentWriteAlias(indexSet, deflectorIndices, indexName))
                .filter(indexName -> exceedsAgeLimit(indexName, cutoffSoft, cutoffHard))
                .count();

        if (removeCount > 0) {
            final String msg = "Running retention for " + removeCount + " aged-out indices.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, IndexRetentionThread.class));

            //runRetention(indexSet, deflectorIndices, removeCount);
        }
    }

    private boolean exceedsAgeLimit(String indexName, long cutoffSoft, long cutoffHard) {
        Optional<DateTime> closingDate = indices.indexClosingDate(indexName);
        if (closingDate.isPresent()) {
            return closingDate.get().isBefore(cutoffSoft + 1);
        }

        Optional<DateTime> creationDate = indices.indexCreationDate(indexName);
        if (creationDate.isPresent()) {
            return creationDate.get().isBefore(cutoffHard + 1);
        }

        LOG.warn(f("Unable to determine creation or closing dates for Index %s - forcing retention", indexName));
        return true;
    }
}
