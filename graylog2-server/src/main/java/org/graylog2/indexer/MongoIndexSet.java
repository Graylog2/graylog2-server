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
package org.graylog2.indexer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.indexer.indices.jobs.SetIndexReadOnlyAndCalculateRangeJob;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
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
    // TODO 3.0: Remove this in 3.0, only used for pre 2.2 backwards compatibility.
    public static final String RESTORED_ARCHIVE_SUFFIX = "_restored_archive";

    public interface Factory {
        MongoIndexSet create(IndexSetConfig config);
    }

    private final IndexSetConfig config;
    private final Indices indices;
    private final Pattern indexPattern;
    private final Pattern deflectorIndexPattern;
    private final String indexWildcard;
    private final IndexRangeService indexRangeService;
    private final AuditEventSender auditEventSender;
    private final NodeId nodeId;
    private final SystemJobManager systemJobManager;
    private final SetIndexReadOnlyAndCalculateRangeJob.Factory jobFactory;
    private final ActivityWriter activityWriter;

    @Inject
    public MongoIndexSet(@Assisted final IndexSetConfig config,
                         final Indices indices,
                         final NodeId nodeId,
                         final IndexRangeService indexRangeService,
                         final AuditEventSender auditEventSender,
                         final SystemJobManager systemJobManager,
                         final SetIndexReadOnlyAndCalculateRangeJob.Factory jobFactory,
                         final ActivityWriter activityWriter
    ) {
        this.config = requireNonNull(config);
        this.indices = requireNonNull(indices);
        this.nodeId = requireNonNull(nodeId);
        this.indexRangeService = requireNonNull(indexRangeService);
        this.auditEventSender = requireNonNull(auditEventSender);
        this.systemJobManager = requireNonNull(systemJobManager);
        this.jobFactory = requireNonNull(jobFactory);
        this.activityWriter = requireNonNull(activityWriter);

        // Part of the pattern can be configured in IndexSetConfig. If set we use the indexMatchPattern from the config.
        if (isNullOrEmpty(config.indexMatchPattern())) {
            // This pattern requires that we check that each index prefix is unique and unambiguous to avoid false matches.
            this.indexPattern = Pattern.compile("^" + config.indexPrefix() + SEPARATOR + "\\d+(?:" + RESTORED_ARCHIVE_SUFFIX + ")?");
            this.deflectorIndexPattern = Pattern.compile("^" + config.indexPrefix() + SEPARATOR + "\\d+");
        } else {
            // This pattern requires that we check that each index prefix is unique and unambiguous to avoid false matches.
            this.indexPattern = Pattern.compile("^" + config.indexMatchPattern() + SEPARATOR + "\\d+(?:" + RESTORED_ARCHIVE_SUFFIX + ")?");
            this.deflectorIndexPattern = Pattern.compile("^" + config.indexMatchPattern() + SEPARATOR + "\\d+");
        }

        // The index wildcard can be configured in IndexSetConfig. If not set we use a default one based on the index
        // prefix.
        if (isNullOrEmpty(config.indexWildcard())) {
            this.indexWildcard = config.indexPrefix() + SEPARATOR + "*";
        } else {
            this.indexWildcard = config.indexWildcard();
        }
    }

    @Override
    public String[] getManagedIndices() {
        final Set<String> indexNames = indices.getIndexNamesAndAliases(getIndexWildcard()).keySet();
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
    public String getIndexWildcard() {
        return indexWildcard;
    }

    @Override
    public String getNewestIndex() throws NoTargetIndexException {
        return buildIndexName(getNewestIndexNumber());
    }

    @VisibleForTesting
    int getNewestIndexNumber() throws NoTargetIndexException {
        final Set<String> indexNames = indices.getIndexNamesAndAliases(getIndexWildcard()).keySet();

        if (indexNames.isEmpty()) {
            throw new NoTargetIndexException("Couldn't find any indices for wildcard " + getIndexWildcard());
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
            throw new NoTargetIndexException("Couldn't get newest index number for indices " + indexNames);
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
        return !isNullOrEmpty(indexName) && !isWriteIndexAlias(indexName) && deflectorIndexPattern.matcher(indexName).matches();
    }

    @Override
    @Nullable
    public String getActiveWriteIndex() throws TooManyAliasesException {
        return indices.aliasTarget(getWriteIndexAlias()).orElse(null);
    }

    @Override
    public Map<String, Set<String>> getAllIndexAliases() {
        final Map<String, Set<String>> indexNamesAndAliases = indices.getIndexNamesAndAliases(getIndexWildcard());

        // filter out the restored archives from the result set
        return indexNamesAndAliases.entrySet().stream()
                .filter(e -> isGraylogDeflectorIndex(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String getIndexPrefix() {
        return config.indexPrefix();
    }

    @Override
    public boolean isUp() {
        return indices.aliasExists(getWriteIndexAlias());
    }

    @Override
    public boolean isWriteIndexAlias(String index) {
        return getWriteIndexAlias().equals(index);
    }

    @Override
    public boolean isManagedIndex(String index) {
        return !isNullOrEmpty(index) && !isWriteIndexAlias(index) && indexPattern.matcher(index).matches();
    }

    @Override
    public void setUp() {
        if (!getConfig().isWritable()) {
            LOG.debug("Not setting up non-writable index set <{}> ({})", getConfig().id(), getConfig().title());
            return;
        }

        // Check if there already is an deflector index pointing somewhere.
        if (isUp()) {
            LOG.info("Found deflector alias <{}>. Using it.", getWriteIndexAlias());
        } else {
            LOG.info("Did not find a deflector alias. Setting one up now.");

            // Do we have a target index to point to?
            try {
                final String currentTarget = getNewestIndex();
                LOG.info("Pointing to already existing index target <{}>", currentTarget);

                pointTo(currentTarget);
            } catch (NoTargetIndexException ex) {
                final String msg = "There is no index target to point to. Creating one now.";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, IndexSet.class));

                cycle(); // No index, so automatically cycling to a new one.
            }
        }
    }

    @Override
    public void cycle() {
        if (!getConfig().isWritable()) {
            LOG.debug("Not cycling non-writable index set <{}> ({})", getConfig().id(), getConfig().title());
            return;
        }

        int oldTargetNumber;

        try {
            oldTargetNumber = getNewestIndexNumber();
        } catch (NoTargetIndexException ex) {
            oldTargetNumber = -1;
        }
        final int newTargetNumber = oldTargetNumber + 1;

        final String newTarget = buildIndexName(newTargetNumber);
        final String oldTarget = buildIndexName(oldTargetNumber);

        if (oldTargetNumber == -1) {
            LOG.info("Cycling from <none> to <{}>.", newTarget);
        } else {
            LOG.info("Cycling from <{}> to <{}>.", oldTarget, newTarget);
        }

        // Create new index.
        LOG.info("Creating target index <{}>.", newTarget);
        if (!indices.create(newTarget, this)) {
            throw new RuntimeException("Could not create new target index <" + newTarget + ">.");
        }

        LOG.info("Waiting for allocation of index <{}>.", newTarget);
        HealthStatus healthStatus = indices.waitForRecovery(newTarget);
        LOG.debug("Health status of index <{}>: {}", newTarget, healthStatus);

        addDeflectorIndexRange(newTarget);
        LOG.info("Index <{}> has been successfully allocated.", newTarget);

        // Point deflector to new index.
        final String indexAlias = getWriteIndexAlias();
        LOG.info("Pointing index alias <{}> to new index <{}>.", indexAlias, newTarget);

        final Activity activity = new Activity(IndexSet.class);
        if (oldTargetNumber == -1) {
            // Only pointing, not cycling.
            pointTo(newTarget);
            activity.setMessage("Cycled index alias <" + indexAlias + "> from <none> to <" + newTarget + ">.");
        } else {
            // Re-pointing from existing old index to the new one.
            LOG.debug("Switching over index alias <{}>.", indexAlias);
            pointTo(newTarget, oldTarget);
            setIndexReadOnlyAndCalculateRange(oldTarget);
            activity.setMessage("Cycled index alias <" + indexAlias + "> from <" + oldTarget + "> to <" + newTarget + ">.");
        }

        LOG.info("Successfully pointed index alias <{}> to index <{}>.", indexAlias, newTarget);

        activityWriter.write(activity);
        auditEventSender.success(AuditActor.system(nodeId), ES_WRITE_INDEX_UPDATE, ImmutableMap.of("indexName", newTarget));
    }

    private void setIndexReadOnlyAndCalculateRange(String indexName) {
        // perform these steps after a delay, so we don't race with indexing into the alias
        // it can happen that an index request still writes to the old deflector target, while we cycled it above.
        // setting the index to readOnly would result in ClusterBlockExceptions in the indexing request.
        // waiting 30 seconds to perform the background task should completely get rid of these errors.
        final SystemJob setIndexReadOnlyAndCalculateRangeJob = jobFactory.create(indexName);
        try {
            systemJobManager.submitWithDelay(setIndexReadOnlyAndCalculateRangeJob, 30, TimeUnit.SECONDS);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Cannot set index <" + indexName + "> to read only and calculate its range. It won't be optimized.", e);
        }
    }

    private void addDeflectorIndexRange(String indexName) {
        final IndexRange deflectorRange = indexRangeService.createUnknownRange(indexName);
        indexRangeService.save(deflectorRange);
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
    public void pointTo(String newIndexName, String oldIndexName) {
        indices.cycleAlias(getWriteIndexAlias(), newIndexName, oldIndexName);
    }

    private void pointTo(final String indexName) {
        indices.cycleAlias(getWriteIndexAlias(), indexName);
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
