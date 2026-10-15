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

            // Another node may have finished between the check above and the lock being taken.
            if (clusterConfigService.get(MigrationCompleted.class) != null) {
                LOG.debug("Migration completed by another node.");
                return;
            }

            repairAffectedIndexSets();
        } catch (AlreadyLockedException e) {
            LOG.debug("Another node is already repairing index sets with numerically mapped date fields.");
        } catch (Exception e) {
            // A migration that throws takes the server down with it, see ServerBootstrap#startCommand. Nothing here
            // is worth that: ingestion works either way, the affected messages simply keep failing until the next
            // attempt. Leaving the completion marker unwritten is what makes that attempt happen.
            LOG.error("Could not repair index sets with numerically mapped date fields. Retrying on the next "
                    + "startup. Until then, messages with such a field keep failing to index.", e);
        }
    }

    private void repairAffectedIndexSets() {
        int rotated = 0;
        int incomplete = 0;

        for (IndexSetConfig indexSetConfig : indexSetService.findAll()) {
            if (!indexSetConfig.isWritable()) {
                continue;
            }
            if (!indexSetConfig.canHaveCustomFieldMappings()) {
                // Index sets with immutable field types - events, failures and the like - reject the mapping change,
                // so rotating them could not repair anything and would only cost an index on every startup.
                LOG.debug("Skipping index set <{}> ({}), its field types cannot be changed.",
                        indexSetConfig.id(), indexSetConfig.title());
                continue;
            }
            try {
                final Outcome outcome = rotateIfAffected(indexSetConfig);
                if (outcome == Outcome.ROTATED || outcome == Outcome.ROTATED_WITH_ERRORS) {
                    rotated++;
                }
                if (outcome == Outcome.ROTATED_WITH_ERRORS || outcome == Outcome.NOT_REPAIRABLE) {
                    incomplete++;
                }
            } catch (Exception e) {
                incomplete++;
                LOG.error("Could not check or rotate index set <{}> ({}). Messages with a date field that this "
                                + "index set maps numerically will keep failing to index until it is rotated.",
                        indexSetConfig.id(), indexSetConfig.title(), e);
            }
        }

        if (incomplete > 0) {
            LOG.warn("Rotated {} index set(s). {} index set(s) were left unfinished and are retried on the next "
                    + "startup.", rotated, incomplete);
            return;
        }

        clusterConfigService.write(new MigrationCompleted(rotated));
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

        final List<String> pinned = pinToDate(indexSetConfig, findings);
        if (pinned.isEmpty()) {
            // Rotating now would only produce an index that maps the same fields the same way again, and the next
            // startup would do it once more. Report it as unfinished and leave the indices alone.
            LOG.warn("Not rotating index set <{}> ({}): none of its affected fields could be set to date.",
                    indexSetConfig.id(), indexSetConfig.title());
            return Outcome.NOT_REPAIRABLE;
        }

        // The mappings were written to the index set config, so the index set has to be read again: the new index is
        // created from the configuration this object holds, and the one captured above no longer has them.
        indexSetService.get(indexSetConfig.id())
                .map(mongoIndexSetFactory::create)
                .orElse(indexSet)
                .cycle();

        notifyAboutRotation(indexSetConfig, pinned);

        return pinned.size() == findings.size() ? Outcome.ROTATED : Outcome.ROTATED_WITH_ERRORS;
    }

    private enum Outcome {
        NOT_AFFECTED,
        ROTATED,
        /**
         * Rotated, but at least one field could not be pinned, so its type is left to the first value that arrives.
         * The migration treats this as unfinished and tries again on the next startup.
         */
        ROTATED_WITH_ERRORS,
        /**
         * Affected, but not a single field could be pinned, so rotating would achieve nothing. Nothing was changed.
         */
        NOT_REPAIRABLE
    }

    /**
     * Pins the affected fields to {@code date} before rotating, so that the new index maps them deterministically
     * instead of leaving it to whichever value the first document happens to carry. A date mapping accepts both the
     * dates Graylog writes now and the epoch milliseconds an older source may still send.
     * <p>
     * Existing custom field mappings are kept: the mappings are merged, and the detector ignores fields that already
     * have one.
     *
     * @return the fields that were pinned. Anything missing from it is left to dynamic mapping, where the next value
     * that arrives decides its type again, so the caller has to treat that as unfinished work.
     */
    private List<String> pinToDate(IndexSetConfig indexSetConfig, List<NumericDateFieldDetector.Finding> findings) {
        final List<String> pinned = new ArrayList<>();
        for (NumericDateFieldDetector.Finding finding : findings) {
            try {
                fieldTypeMappingsService.changeFieldType(
                        new CustomFieldMapping(finding.fieldName(), DATE_FIELD_TYPE),
                        Set.of(indexSetConfig.id()),
                        // rotate once at the end instead of once per field
                        false);
                pinned.add(finding.fieldName());
            } catch (Exception e) {
                LOG.warn("Could not set the type of field <{}> in index set <{}> to date. The index set is still "
                                + "rotated, but the type of that field is determined by the first message written "
                                + "to the new index. Retrying on the next startup.",
                        finding.fieldName(), indexSetConfig.id(), e);
            }
        }
        return pinned;
    }

    /**
     * Reports what was changed, per index set, so that an administrator can see which fields were given a custom
     * type and undo one if it was not wanted. The key is the index set, so that an index set repaired by a later
     * run gets its own notification instead of being swallowed as a duplicate of the first one.
     */
    private void notifyAboutRotation(IndexSetConfig indexSetConfig, List<String> pinnedFields) {
        if (pinnedFields.isEmpty()) {
            return;
        }
        try {
            final Notification notification = notificationService.buildNow()
                    .addType(Notification.Type.GENERIC)
                    .addKey(LOCK_RESOURCE + ":" + indexSetConfig.id())
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("title", "Index set " + indexSetConfig.title()
                            + " rotated to restore date field mappings")
                    .addDetail("description", "The index set " + indexSetConfig.title() + " mapped these "
                            + "fields as numbers, which earlier versions wrote for date values: "
                            + String.join(", ", pinnedFields) + ". They were given the custom field type date and "
                            + "the index set was rotated, so that its new index maps them as dates again. Existing "
                            + "custom field types were kept. "
                            + "Until the older indices are removed by their retention strategy, those fields still "
                            + "have a numeric type there, so a search covering both the old and the new indices "
                            + "reports a field type conflict for them. That resolves itself once the older indices "
                            + "are gone; reindexing them clears it sooner. Messages already written keep their "
                            + "numeric values.");
            notificationService.publishIfFirst(notification);
        } catch (Exception e) {
            LOG.warn("Could not publish the notification about the rotation of index set <{}>.",
                    indexSetConfig.id(), e);
        }
    }

    public record MigrationCompleted(int rotatedIndexSets) {}
}
