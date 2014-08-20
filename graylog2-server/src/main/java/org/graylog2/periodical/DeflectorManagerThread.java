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
import org.graylog2.Configuration;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.initializers.IndexerSetupService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DeflectorManagerThread extends Periodical { // public class Klimperkiste
    
    private static final Logger LOG = LoggerFactory.getLogger(DeflectorManagerThread.class);

    private NotificationService notificationService;
    private final Indexer indexer;
    private final Deflector deflector;
    private final Configuration configuration;
    private final ActivityWriter activityWriter;
    private final IndexerSetupService indexerSetupService;

    @Inject
    public DeflectorManagerThread(NotificationService notificationService,
                                  Indexer indexer,
                                  Deflector deflector,
                                  Configuration configuration,
                                  ActivityWriter activityWriter,
                                  IndexerSetupService indexerSetupService) {
        this.notificationService = notificationService;
        this.indexer = indexer;
        this.deflector = deflector;
        this.configuration = configuration;
        this.activityWriter = activityWriter;
        this.indexerSetupService = indexerSetupService;
    }

    @Override
    public void doRun() {
        // Point deflector to a new index if required.
        try {
            if (indexerSetupService.isRunning()) {
                checkAndRepair();
                point();
            }
        } catch (Exception e) {
            LOG.error("Couldn't point deflector to a new index", e);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private void point() {
        // Check if message limit of current target is hit. Point to new target if so.
        String currentTarget;
        long messageCountInTarget = 0;
        
        try {
            currentTarget = deflector.getNewestTargetName(indexer);
            messageCountInTarget = indexer.indices().numberOfMessages(currentTarget);
        } catch(Exception e) {
            LOG.error("Tried to check for number of messages in current deflector target but did not find index. Aborting.", e);
            return;
        }
        
        if (messageCountInTarget > configuration.getElasticSearchMaxDocsPerIndex()) {
            LOG.info("Number of messages in <{}> ({}) is higher than the limit ({}). Pointing deflector to new index now!",
                     currentTarget, messageCountInTarget,
                     configuration.getElasticSearchMaxDocsPerIndex());
            deflector.cycle(indexer);
        } else {
            LOG.debug("Number of messages in <{}> ({}) is lower than the limit ({}). Not doing anything.",
                      currentTarget,messageCountInTarget,
                      configuration.getElasticSearchMaxDocsPerIndex());
        }
    }

    private void checkAndRepair() {
        if (!deflector.isUp(indexer)) {
            if (indexer.indices().exists(deflector.getName())) {
                // Publish a notification if there is an *index* called graylog2_deflector
                Notification notification = notificationService.buildNow()
                        .addType(Notification.Type.DEFLECTOR_EXISTS_AS_INDEX)
                        .addSeverity(Notification.Severity.URGENT);
                final boolean published = notificationService.publishIfFirst(notification);
                if (published) {
                    LOG.warn("There is an index called [" + deflector.getName() + "]. Cannot fix this automatically and published a notification.");
                }
            } else {
                deflector.setUp(indexer);
            }
        } else {
            try {
                String currentTarget = deflector.getCurrentActualTargetIndex(indexer);
                String shouldBeTarget = deflector.getNewestTargetName(indexer);

                if (!currentTarget.equals(shouldBeTarget)) {
                    String msg = "Deflector is pointing to [" + currentTarget + "], not the newest one: [" + shouldBeTarget + "]. Re-pointing.";
                    LOG.warn(msg);
                    activityWriter.write(new Activity(msg, DeflectorManagerThread.class));

                    deflector.pointTo(indexer, shouldBeTarget, currentTarget);
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
