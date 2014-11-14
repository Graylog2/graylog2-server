/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.healing;

import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.buffers.Buffers;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FixDeflectorByMoveJob extends SystemJob {
    public interface Factory {
        FixDeflectorByMoveJob create();
    }

    private static final Logger LOG = LoggerFactory.getLogger(FixDeflectorByMoveJob.class);

    public static final int MAX_CONCURRENCY = 1;
    private final Deflector deflector;
    private final ServerStatus serverStatus;
    private final Indices indices;
    private final ActivityWriter activityWriter;
    private final Buffers bufferSynchronizer;
    private final NotificationService notificationService;

    private boolean cancelRequested = false;
    private int progress = 0;

    @AssistedInject
    public FixDeflectorByMoveJob(Deflector deflector,
                                 Indices indices,
                                 ServerStatus serverStatus,
                                 ActivityWriter activityWriter,
                                 Buffers bufferSynchronizer,
                                 NotificationService notificationService) {
        super(serverStatus);
        this.deflector = deflector;
        this.indices = indices;
        this.serverStatus = serverStatus;
        this.activityWriter = activityWriter;
        this.bufferSynchronizer = bufferSynchronizer;
        this.notificationService = notificationService;
    }

    @Override
    public void execute() {
        if (deflector.isUp() || !indices.exists(deflector.getName())) {
            LOG.error("There is no index <{}>. No need to run this job. Aborting.", deflector.getName());
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
                newTarget = deflector.getNewestTargetName();

                LOG.info("Starting to move <{}> to <{}>.", deflector.getName(), newTarget);
                indices.move(deflector.getName(), newTarget);
            } catch(Exception e) {
                LOG.error("Moving index failed. Rolling back.", e);
                if (newTarget != null) {
                    indices.delete(newTarget);
                }
                throw new RuntimeException(e);
            }

            LOG.info("Done moving deflector index.");

            progress = 85;

            // Delete deflector index.
            LOG.info("Deleting <{}> index.", deflector.getName());
            indices.delete(deflector.getName());
            progress = 90;

            // Set up deflector.
            deflector.setUp();
            progress = 95;
        } finally {
            // Start message processing again.
            try {
                serverStatus.unlockProcessingPause();

                if (wasProcessing) {
                    serverStatus.resumeMessageProcessing();
                }
            } catch (Exception e) {
                // lol checked exceptions
                throw new RuntimeException("Could not unlock processing pause.", e);
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
        this.cancelRequested = true;
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
        return true;
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
