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
package org.graylog2.migrations;

import jakarta.inject.Inject;
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.ClusterLockResources;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.graylog2.indexer.fieldtypes.datefix.NumericDateFieldDetector;
import org.graylog2.indexer.fieldtypes.mapping.FieldTypeMappingsService;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.MongoIndexSet;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Rotates index sets whose write index maps a date field numerically.
 * <p>
 * Graylog 7.1 serialized date values as numbers until the storage client was fixed to use Graylog's own object
 * mapper. An index written in the meantime mapped such a field as {@code long} or {@code float}, and now that
 * Graylog writes dates again that mapping rejects every message carrying the field. The type of a field cannot be
 * changed in an existing index, so the only way to get back to a date mapping is a new index.
 * <p>
 * Only index sets that are actually affected are rotated, see {@link NumericDateFieldDetector} for how they are
 * recognised. Indices that already exist keep their numeric values; correcting those needs a reindex and is out of
 * scope here.
 * <p>
 * Like {@code V20260902150000_RotateCollectorLogsIndexSet}, a failure does not abort server startup. Ingestion keeps
 * working, the affected messages simply keep failing until the next regular rotation, so the migration logs the
 * problem and tries again on the next startup by not writing its completion marker.
 */
public class V20261014120000_RotateIndexSetsWithNumericDateFields extends Migration {
    private static final Logger LOG =
            LoggerFactory.getLogger(V20261014120000_RotateIndexSetsWithNumericDateFields.class);

    /**
     * The one type that takes both representations: the dates Graylog writes now and the epoch milliseconds that
     * earlier versions wrote. A custom field mapping creates it without a format restriction.
     */
    private static final String DATE_FIELD_TYPE = "date";

    private static final String LOCK_RESOURCE = "numeric-date-field-repair";

    private final ClusterConfigService clusterConfigService;
    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory mongoIndexSetFactory;
    private final NumericDateFieldDetector detector;
    private final NotificationService notificationService;
    private final FieldTypeMappingsService fieldTypeMappingsService;
    private final RefreshingLockService.Factory lockServiceFactory;

    @Inject
    public V20261014120000_RotateIndexSetsWithNumericDateFields(ClusterConfigService clusterConfigService,
                                                                IndexSetService indexSetService,
                                                                MongoIndexSet.Factory mongoIndexSetFactory,
                                                                NumericDateFieldDetector detector,
                                                                NotificationService notificationService,
                                                                FieldTypeMappingsService fieldTypeMappingsService,
                                                                RefreshingLockService.Factory lockServiceFactory) {
        this.clusterConfigService = clusterConfigService;
        this.indexSetService = indexSetService;
        this.mongoIndexSetFactory = mongoIndexSetFactory;
        this.detector = detector;
        this.notificationService = notificationService;
        this.fieldTypeMappingsService = fieldTypeMappingsService;
        this.lockServiceFactory = lockServiceFactory;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-10-14T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        // Standard migrations run on every node that considers itself leader, and with automatic leader election
        // that is decided after the migrations have run. Without a cluster-wide lock two nodes could detect the same
        // index set and rotate it twice, which costs an index and brings count-based retention forward for nothing.
        try (var lockService = lockServiceFactory.create()) {
            lockService.acquireAndKeepLock(ClusterLockResources.name(LOCK_RESOURCE), UUID.randomUUID().toString());

            // Another node may have finished while we were waiting for the lock.
            if (clusterConfigService.get(MigrationCompleted.class) != null) {
                LOG.debug("Migration completed by another node.");
                return;
            }

            repairAffectedIndexSets();
        } catch (AlreadyLockedException e) {
            LOG.debug("Another node is already repairing index sets with numerically mapped date fields.");
        }
    }

    private void repairAffectedIndexSets() {
        final List<String> rotatedIndexSets = new ArrayList<>();
        int failed = 0;

        for (IndexSetConfig indexSetConfig : indexSetService.findAll()) {
            if (!indexSetConfig.isWritable()) {
                continue;
            }
            try {
                final Outcome outcome = rotateIfAffected(indexSetConfig);
                if (outcome != Outcome.NOT_AFFECTED) {
                    rotatedIndexSets.add(indexSetConfig.title());
                }
                if (outcome == Outcome.ROTATED_WITH_ERRORS) {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                LOG.error("Could not check or rotate index set <{}> ({}). Messages with a date field that this "
                                + "index set maps numerically will keep failing to index until it is rotated.",
                        indexSetConfig.id(), indexSetConfig.title(), e);
            }
        }

        if (!rotatedIndexSets.isEmpty()) {
            LOG.info("Rotated {} index set(s) that mapped a date field numerically: {}",
                    rotatedIndexSets.size(), String.join(", ", rotatedIndexSets));
            notifyAboutRotation(rotatedIndexSets);
        }

        if (failed > 0) {
            LOG.warn("{} index set(s) could not be handled. Retrying on the next startup.", failed);
            return;
        }

        clusterConfigService.write(new MigrationCompleted(rotatedIndexSets.size()));
    }

    private Outcome rotateIfAffected(IndexSetConfig indexSetConfig) throws Exception {
        final IndexSet indexSet = mongoIndexSetFactory.create(indexSetConfig);
        final String writeIndex = indexSet.getActiveWriteIndex();
        if (writeIndex == null) {
            // Nothing has been written to this index set yet, so its next index is created from the current template.
            return Outcome.NOT_AFFECTED;
        }

        final List<NumericDateFieldDetector.Finding> findings =
                detector.findNumericDateFields(indexSetConfig, writeIndex);
        if (findings.isEmpty()) {
            return Outcome.NOT_AFFECTED;
        }

        LOG.info("Rotating index set <{}> ({}): index <{}> maps {} numerically.",
                indexSetConfig.id(), indexSetConfig.title(), writeIndex,
                findings.stream().map(NumericDateFieldDetector.Finding::describe).collect(Collectors.joining(", ")));

        final boolean allPinned = pinToDate(indexSetConfig, findings);

        // The mappings were written to the index set config, so the index set has to be read again: the new index is
        // created from the configuration this object holds, and the one captured above no longer has them.
        indexSetService.get(indexSetConfig.id())
                .map(mongoIndexSetFactory::create)
                .orElse(indexSet)
                .cycle();

        return allPinned ? Outcome.ROTATED : Outcome.ROTATED_WITH_ERRORS;
    }

    private enum Outcome {
        NOT_AFFECTED,
        ROTATED,
        /**
         * Rotated, but at least one field could not be pinned, so its type is left to the first value that arrives.
         * The migration treats this as unfinished and tries again on the next startup.
         */
        ROTATED_WITH_ERRORS
    }

    /**
     * Pins the affected fields to {@code date} before rotating, so that the new index maps them deterministically
     * instead of leaving it to whichever value the first document happens to carry. A date mapping accepts both the
     * dates Graylog writes now and the epoch milliseconds an older source may still send.
     * <p>
     * Existing custom field mappings are kept: the mappings are merged, and the detector ignores fields that already
     * have one.
     *
     * @return whether every field could be pinned. A field that could not is left to dynamic mapping, where the next
     * value that arrives decides its type again, so the caller has to treat that as unfinished work.
     */
    private boolean pinToDate(IndexSetConfig indexSetConfig, List<NumericDateFieldDetector.Finding> findings) {
        boolean allPinned = true;
        for (NumericDateFieldDetector.Finding finding : findings) {
            try {
                fieldTypeMappingsService.changeFieldType(
                        new CustomFieldMapping(finding.fieldName(), DATE_FIELD_TYPE),
                        Set.of(indexSetConfig.id()),
                        // rotate once at the end instead of once per field
                        false);
            } catch (Exception e) {
                allPinned = false;
                LOG.warn("Could not set the type of field <{}> in index set <{}> to date. The index set is still "
                                + "rotated, but the type of that field is determined by the first message written "
                                + "to the new index. Retrying on the next startup.",
                        finding.fieldName(), indexSetConfig.id(), e);
            }
        }
        return allPinned;
    }

    /**
     * A rotation is not invisible: the new index starts with an empty dynamic mapping, so a field that receives
     * values of more than one type can end up mapped differently than it was in the previous index. Telling the
     * administrator which index sets were rotated gives them something to correlate such a change with.
     */
    private void notifyAboutRotation(List<String> rotatedIndexSets) {
        try {
            final Notification notification = notificationService.buildNow()
                    .addType(Notification.Type.GENERIC)
                    .addKey("numeric-date-field-rotation")
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("title", "Index sets rotated to restore date field mappings")
                    .addDetail("description", "The following index sets mapped a date field as a number, which "
                            + "earlier versions wrote for date values, and were rotated so that their new index maps "
                            + "it as a date again: " + String.join(", ", rotatedIndexSets) + ". "
                            + "Those fields were set to the type date, existing custom field types were kept. "
                            + "Until the older indices are removed by their retention strategy, those fields still "
                            + "have a numeric type there, so a search covering both the old and the new indices "
                            + "reports a field type conflict for them. That resolves itself once the older indices "
                            + "are gone; reindexing them clears it sooner. Messages already written keep their "
                            + "numeric values.");
            notificationService.publishIfFirst(notification);
        } catch (Exception e) {
            LOG.warn("Could not publish the notification about rotated index sets.", e);
        }
    }

    public record MigrationCompleted(int rotatedIndexSets) {}
}
