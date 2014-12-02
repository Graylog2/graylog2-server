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
package org.graylog2.periodical;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.initializers.IndexerSetupService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexRotationThread extends Periodical {
    
    private static final Logger LOG = LoggerFactory.getLogger(IndexRotationThread.class);

    private NotificationService notificationService;
    private final Deflector deflector;
    private final ActivityWriter activityWriter;
    private final IndexerSetupService indexerSetupService;
    private final Indices indices;
    private final Provider<RotationStrategy> rotationStrategyProvider;

    @Inject
    public IndexRotationThread(NotificationService notificationService,
                               Indices indices,
                               Deflector deflector,
                               ActivityWriter activityWriter,
                               IndexerSetupService indexerSetupService,
                               Provider<RotationStrategy> rotationStrategyProvider) {
        this.notificationService = notificationService;
        this.deflector = deflector;
        this.activityWriter = activityWriter;
        this.indexerSetupService = indexerSetupService;
        this.indices = indices;
        this.rotationStrategyProvider = rotationStrategyProvider;
    }

    @Override
    public void doRun() {
        // Point deflector to a new index if required.
        try {
            if (indexerSetupService.isRunning()) {
                checkAndRepair();
                checkForRotation();
            }
        } catch (Exception e) {
            LOG.error("Couldn't point deflector to a new index", e);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected void checkForRotation() {
        final RotationStrategy rotationStrategy = rotationStrategyProvider.get();

        String currentTarget;
        try {
            currentTarget = deflector.getNewestTargetName();
        } catch (NoTargetIndexException e) {
            LOG.error("Could not find current deflector target. Aborting.", e);
            return;
        }
        final RotationStrategy.Result rotate = rotationStrategy.shouldRotate(currentTarget);
        if (rotate == null) {
            LOG.error("Cannot perform rotation at this moment.");
            return;
        }
        LOG.debug("Rotation strategy result: {}", rotate.getDescription());
        if (rotate.shouldRotate()) {
            LOG.info("Deflector index <{}> should be rotated, Pointing deflector to new index now!", currentTarget);
            deflector.cycle();
        } else {
            LOG.debug("Deflector index <{}> should not be rotated. Not doing anything.", currentTarget);
        }
    }

    protected void checkAndRepair() {
        if (!deflector.isUp()) {
            if (indices.exists(deflector.getName())) {
                // Publish a notification if there is an *index* called graylog2_deflector
                Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.DEFLECTOR_EXISTS_AS_INDEX)
                        .addSeverity(Notification.Severity.URGENT);
                final boolean published = notificationService.publishIfFirst(notification);
                if (published) {
                    LOG.warn("There is an index called [" + deflector.getName() + "]. Cannot fix this automatically and published a notification.");
                }
            } else {
                deflector.setUp();
            }
        } else {
            try {
                String currentTarget = deflector.getCurrentActualTargetIndex();
                String shouldBeTarget = deflector.getNewestTargetName();

                if (!currentTarget.equals(shouldBeTarget)) {
                    String msg = "Deflector is pointing to [" + currentTarget + "], not the newest one: [" + shouldBeTarget + "]. Re-pointing.";
                    LOG.warn(msg);
                    activityWriter.write(new Activity(msg, IndexRotationThread.class));

                    deflector.pointTo(shouldBeTarget, currentTarget);
                }
            } catch (NoTargetIndexException e) {
                LOG.warn("Deflector is not up. Not trying to point to another index.");
            }
        }

    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 10;
    }

}
