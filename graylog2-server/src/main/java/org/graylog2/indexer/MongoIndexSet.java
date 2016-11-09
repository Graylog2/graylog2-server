/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.assistedinject.Assisted;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.indices.InvalidAliasNameException;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.indexer.indices.jobs.SetIndexReadOnlyAndCalculateRangeJob;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;
import static org.graylog2.audit.AuditEventTypes.ES_WRITE_INDEX_UPDATE;

public class MongoIndexSet implements IndexSet {
    private static final Logger LOG = LoggerFactory.getLogger(MongoIndexSet.class);

    public static final String SEPARATOR = "_";
    public static final String DEFLECTOR_SUFFIX = "deflector";

    // TODO: Hardcoded archive suffix. See: https://github.com/Graylog2/graylog2-server/issues/2058
    public static final String RESTORED_ARCHIVE_SUFFIX = "_restored_archive";

    public interface Factory {
        MongoIndexSet create(IndexSetConfig config);
    }

    private final IndexSetConfig config;
    private final Indices indices;
    private final Pattern indexPattern;
    private final Pattern deflectorIndexPattern;
    private final AuditEventSender auditEventSender;
    private final NodeId nodeId;
    private final SystemJobManager systemJobManager;
    private final SetIndexReadOnlyAndCalculateRangeJob.Factory jobFactory;
    private final ActivityWriter activityWriter;

    @Inject
    public MongoIndexSet(@Assisted final IndexSetConfig config,
                         final Indices indices,
                         final NodeId nodeId,
                         final AuditEventSender auditEventSender,
                         final SystemJobManager systemJobManager,
                         final SetIndexReadOnlyAndCalculateRangeJob.Factory jobFactory,
                         final ActivityWriter activityWriter
    ) {
        this.config = requireNonNull(config);
        this.indices = requireNonNull(indices);
        this.nodeId = requireNonNull(nodeId);
        this.auditEventSender = requireNonNull(auditEventSender);
        this.systemJobManager = requireNonNull(systemJobManager);
        this.jobFactory = requireNonNull(jobFactory);
        this.activityWriter = requireNonNull(activityWriter);

        this.indexPattern = Pattern.compile("^" + config.indexPrefix() + SEPARATOR + "\\d+(?:" + RESTORED_ARCHIVE_SUFFIX + ")?");
        this.deflectorIndexPattern = Pattern.compile("^" + config.indexPrefix() + SEPARATOR + "\\d+");
    }

    @Override
    public String[] getManagedIndicesNames() {
        final Set<String> indexNames = indices.getIndexNamesAndAliases(getWriteIndexWildcard()).keySet();
        // also allow restore archives to be returned
        final List<String> result = indexNames.stream()
                .filter(this::isManagedIndex)
                .collect(Collectors.toList());

        return result.toArray(new String[result.size()]);
    }

    @Override
    public String getWriteIndexAlias() {
        return config.indexPrefix() + SEPARATOR + DEFLECTOR_SUFFIX;
    }

    @Override
    public String getWriteIndexWildcard() {
        return config.indexPrefix() + SEPARATOR + "*";
    }

    @Override
    public String getNewestTargetName() throws NoTargetIndexException {
        return buildIndexName(getNewestTargetNumber());
    }

    @VisibleForTesting
    int getNewestTargetNumber() throws NoTargetIndexException {
        final Set<String> indexNames = indices.getIndexNamesAndAliases(getWriteIndexWildcard()).keySet();

        if (indexNames.isEmpty()) {
            throw new NoTargetIndexException();
        }

        int highestIndexNumber = -1;
        for (String indexName : indexNames) {
            if (!isGraylogDeflectorIndex(indexName)) {
                continue;
            }

            final int currentHighest = highestIndexNumber;
            highestIndexNumber = extractIndexNumber(indexName)
                    .map(indexNumber -> Math.max(indexNumber, currentHighest))
                    .orElse(highestIndexNumber);
        }

        if (highestIndexNumber == -1) {
            throw new NoTargetIndexException();
        }

        return highestIndexNumber;
    }

    @Override
    public Optional<Integer> extractIndexNumber(final String indexName) {
        final int beginIndex = config.indexPrefix().length() + 1;
        if (indexName.length() < beginIndex) {
            return Optional.empty();
        }

        final String suffix = indexName.substring(beginIndex);
        try {
            return Optional.of(Integer.parseInt(suffix));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @VisibleForTesting
    String buildIndexName(final int number) {
        return config.indexPrefix() + SEPARATOR + number;
    }

    @VisibleForTesting
    boolean isGraylogDeflectorIndex(final String indexName) {
        return !isNullOrEmpty(indexName) && !isDeflectorAlias(indexName) && deflectorIndexPattern.matcher(indexName).matches();
    }

    @Override
    public String getCurrentActualTargetIndex() throws TooManyAliasesException {
        return indices.aliasTarget(getWriteIndexAlias());
    }

    @Override
    public Map<String, Set<String>> getAllDeflectorAliases() {
        final Map<String, Set<String>> indexNamesAndAliases = indices.getIndexNamesAndAliases(getWriteIndexWildcard());

        // filter out the restored archives from the result set
        return indexNamesAndAliases.entrySet().stream()
                .filter(e -> isGraylogDeflectorIndex(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public boolean isUp() {
        return indices.aliasExists(getWriteIndexAlias());
    }

    @Override
    public boolean isDeflectorAlias(String index) {
        return getWriteIndexAlias().equals(index);
    }

    @Override
    public boolean isManagedIndex(String index) {
        return !isNullOrEmpty(index) && !isDeflectorAlias(index) && indexPattern.matcher(index).matches();
    }

    @Override
    public void setUp() {
        // Check if there already is an deflector index pointing somewhere.
        if (isUp()) {
            LOG.info("Found deflector alias <{}>. Using it.", getWriteIndexAlias());
        } else {
            LOG.info("Did not find an deflector alias. Setting one up now.");

            // Do we have a target index to point to?
            try {
                final String currentTarget = getNewestTargetName();
                LOG.info("Pointing to already existing index target <{}>", currentTarget);

                pointTo(currentTarget);
            } catch (NoTargetIndexException ex) {
                final String msg = "There is no index target to point to. Creating one now.";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, IndexSet.class));

                cycle(); // No index, so automatically cycling to a new one.
            } catch (InvalidAliasNameException e) {
                LOG.error("Seems like there already is an index called [{}]", getWriteIndexAlias());
            }
        }
    }

    @Override
    public void cycle() {
        LOG.info("Cycling deflector to next index now.");
        int oldTargetNumber;

        try {
            oldTargetNumber = getNewestTargetNumber();
        } catch (NoTargetIndexException ex) {
            oldTargetNumber = -1;
        }
        final int newTargetNumber = oldTargetNumber + 1;

        final String newTarget = buildIndexName(newTargetNumber);
        final String oldTarget = buildIndexName(oldTargetNumber);

        if (oldTargetNumber == -1) {
            LOG.info("Cycling from <none> to <{}>", newTarget);
        } else {
            LOG.info("Cycling from <{}> to <{}>", oldTarget, newTarget);
        }

        // Create new index.
        LOG.info("Creating index target <{}>...", newTarget);
        if (!indices.create(newTarget)) {
            LOG.error("Could not properly create new target <{}>", newTarget);
        }

        LOG.info("Waiting for index allocation of <{}>", newTarget);
        ClusterHealthStatus healthStatus = indices.waitForRecovery(newTarget);
        LOG.debug("Health status of index <{}>: {}", newTarget, healthStatus);

        // addDeflectorIndexRange(newTarget);
        LOG.info("Done!");

        // Point deflector to new index.
        LOG.info("Pointing deflector to new target index....");

        final Activity activity = new Activity(IndexSet.class);
        if (oldTargetNumber == -1) {
            // Only pointing, not cycling.
            pointTo(newTarget);
            activity.setMessage("Cycled deflector from <none> to <" + newTarget + ">");
        } else {
            // Re-pointing from existing old index to the new one.
            LOG.debug("Now switching over deflector alias.");
            pointTo(newTarget, oldTarget);
            calculateRange(oldTarget);
            activity.setMessage("Cycled deflector from <" + oldTarget + "> to <" + newTarget + ">");
        }

        LOG.info("Done!");

        activityWriter.write(activity);
        auditEventSender.success(AuditActor.system(nodeId), ES_WRITE_INDEX_UPDATE, ImmutableMap.of("indexName", newTarget));
    }

    private void calculateRange(String oldTarget) {
        // perform these steps after a delay, so we don't race with indexing into the alias
        // it can happen that an index request still writes to the old deflector target, while we cycled it above.
        // setting the index to readOnly would result in ClusterBlockExceptions in the indexing request.
        // waiting 30 seconds to perform the background task should completely get rid of these errors.
        final SystemJob setIndexReadOnlyAndCalculateRangeJob = jobFactory.create(oldTarget);
        try {
            systemJobManager.submitWithDelay(setIndexReadOnlyAndCalculateRangeJob, 30, TimeUnit.SECONDS);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Cannot set index <" + oldTarget + "> to read only and calculate its range. It won't be optimized.", e);
        }
    }

    @Override
    public void cleanupAliases(Set<String> indexNames) {
        final SortedSet<String> sortedSet = ImmutableSortedSet
                .orderedBy(new IndexNameComparator(this))
                .addAll(indexNames)
                .build();

        indices.removeAliases(getWriteIndexAlias(), sortedSet.headSet(sortedSet.last()));
    }

    @Override
    public void pointTo(String shouldBeTarget, String currentTarget) {
        indices.cycleAlias(getWriteIndexAlias(), shouldBeTarget, currentTarget);
    }

    private void pointTo(final String newIndex) {
        indices.cycleAlias(getWriteIndexAlias(), newIndex);
    }

    @Override
    public IndexSetConfig getConfig() {
        return config;
    }

    @Override
    public int compareTo(IndexSet o) {
        return ComparisonChain.start()
                .compare(this.getConfig(), o.getConfig())
                .result();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MongoIndexSet that = (MongoIndexSet) o;
        return Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return config.hashCode();
    }

    @Override
    public String toString() {
        return "MongoIndexSet{" + "config=" + config + '}';
    }
}
