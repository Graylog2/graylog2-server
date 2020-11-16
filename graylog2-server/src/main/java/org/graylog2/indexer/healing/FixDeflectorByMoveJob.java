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
package org.graylog2.indexer.healing;

import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.buffers.Buffers;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixDeflectorByMoveJob extends SystemJob {
    public interface Factory {
        FixDeflectorByMoveJob create();
    }

    private static final Logger LOG = LoggerFactory.getLogger(FixDeflectorByMoveJob.class);

    public static final int MAX_CONCURRENCY = 1;
    private final IndexSetRegistry indexSetRegistry;
    private final ServerStatus serverStatus;
    private final Indices indices;
    private final ActivityWriter activityWriter;
    private final Buffers bufferSynchronizer;
    private final NotificationService notificationService;

    private int progress = 0;

    @AssistedInject
    public FixDeflectorByMoveJob(IndexSetRegistry indexSetRegistry,
                                 Indices indices,
                                 ServerStatus serverStatus,
                                 ActivityWriter activityWriter,
                                 Buffers bufferSynchronizer,
                                 NotificationService notificationService) {
        this.indexSetRegistry = indexSetRegistry;
        this.indices = indices;
        this.serverStatus = serverStatus;
        this.activityWriter = activityWriter;
        this.bufferSynchronizer = bufferSynchronizer;
        this.notificationService = notificationService;
    }

    @Override
    public void execute() {
        indexSetRegistry.forEach(this::doExecute);
    }

    public void doExecute(IndexSet indexSet) {
        if (!indexSet.getConfig().isWritable()) {
            LOG.debug("No need to fix deflector for non-writable index set <{}> ({})", indexSet.getConfig().id(),
                    indexSet.getConfig().title());
            return;
        }

        if (indexSet.isUp() || !indices.exists(indexSet.getWriteIndexAlias())) {
            LOG.error("There is no index <{}>. No need to run this job. Aborting.", indexSet.getWriteIndexAlias());
            return;
        }

        LOG.info("Attempting to fix deflector with move strategy.");

        boolean wasProcessing = true;
        try {
            // Pause message processing and lock the pause.
            wasProcessing = serverStatus.isProcessing();
            serverStatus.pauseMessageProcessing();
            progress = 5;

            bufferSynchronizer.waitForEmptyBuffers();
            progress = 10;

            // Copy messages to new index.
            String newTarget = null;
            try {
                newTarget = indexSet.getNewestIndex();

                LOG.info("Starting to move <{}> to <{}>.", indexSet.getWriteIndexAlias(), newTarget);
                indices.move(indexSet.getWriteIndexAlias(), newTarget);
            } catch (Exception e) {
                LOG.error("Moving index failed. Rolling back.", e);
                if (newTarget != null) {
                    indices.delete(newTarget);
                }
                throw new RuntimeException(e);
            }

            LOG.info("Done moving deflector index.");

            progress = 85;

            // Delete deflector index.
            LOG.info("Deleting <{}> index.", indexSet.getWriteIndexAlias());
            indices.delete(indexSet.getWriteIndexAlias());
            progress = 90;

            // Set up deflector.
            indexSet.setUp();
            progress = 95;
        } finally {
            // Start message processing again.
            serverStatus.unlockProcessingPause();

            if (wasProcessing) {
                serverStatus.resumeMessageProcessing();
            }
        }

        progress = 90;
        activityWriter.write(new Activity("Notification condition [" + Notification.Type.DEFLECTOR_EXISTS_AS_INDEX + "] " +
                "has been fixed.", this.getClass()));
        notificationService.fixed(Notification.Type.DEFLECTOR_EXISTS_AS_INDEX);

        progress = 100;
        LOG.info("Finished.");
    }

    @Override
    public void requestCancel() {
        // Cannot be canceled.
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int maxConcurrency() {
        return MAX_CONCURRENCY;
    }

    @Override
    public boolean providesProgress() {
        return true;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Tries to fix a broken deflector alias by converting the deflector index to a valid index. Triggered " +
                "by hand after a notification. This operation can take some time depending on the number of messages " +
                "that were already written into the deflector index.";
    }
    @Override
    public String getClassName() {
        return this.getClass().getCanonicalName();
    }

}
