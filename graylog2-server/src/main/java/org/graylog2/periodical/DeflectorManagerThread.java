/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */
package org.graylog2.periodical;

import org.graylog2.system.activities.Activity;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.notifications.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DeflectorManagerThread extends Periodical { // public class Klimperkiste
    
    private static final Logger LOG = LoggerFactory.getLogger(DeflectorManagerThread.class);

    @Override
    public void run() {
        // Point deflector to a new index if required.
        try {
            checkAndRepair();
            point();
        } catch (Exception e) {
            LOG.error("Couldn't point deflector to a new index", e);
        }
    }
    
    private void point() {
        // Check if message limit of current target is hit. Point to new target if so.
        String currentTarget;
        long messageCountInTarget = 0;
        
        try {
            currentTarget = core.getDeflector().getNewestTargetName();
            messageCountInTarget = core.getIndexer().indices().numberOfMessages(currentTarget);
        } catch(Exception e) {
            LOG.error("Tried to check for number of messages in current deflector target but did not find index. Aborting.", e);
            return;
        }
        
        if (messageCountInTarget > core.getConfiguration().getElasticSearchMaxDocsPerIndex()) {
            LOG.info("Number of messages in <{}> ({}) is higher than the limit ({}). Pointing deflector to new index now!",
                    new Object[] {
                            currentTarget, messageCountInTarget,
                            core.getConfiguration().getElasticSearchMaxDocsPerIndex()
                    });
            core.getDeflector().cycle();
        } else {
            LOG.debug("Number of messages in <{}> ({}) is lower than the limit ({}). Not doing anything.",
                    new Object[] {
                            currentTarget,messageCountInTarget,
                            core.getConfiguration().getElasticSearchMaxDocsPerIndex()
                    });
        }
    }

    private void checkAndRepair() {
        if (!core.getDeflector().isUp()) {
            if (core.getIndexer().indices().exists(Deflector.DEFLECTOR_NAME)) {
                // Publish a notification if there is an *index* called graylog2_deflector
                final boolean published = Notification.buildNow(core)
                        .addType(Notification.Type.DEFLECTOR_EXISTS_AS_INDEX)
                        .addSeverity(Notification.Severity.URGENT)
                        .publishIfFirst();
                if (published) {
                    LOG.warn("There is an index called [" + Deflector.DEFLECTOR_NAME + "]. Cannot fix this automatically and published a notification.");
                }
            } else {
                core.getDeflector().setUp();
            }
        } else {
            try {
                String currentTarget = core.getDeflector().getCurrentActualTargetIndex();
                String shouldBeTarget = core.getDeflector().getNewestTargetName();

                if (!currentTarget.equals(shouldBeTarget)) {
                    String msg = "Deflector is pointing to [" + currentTarget + "], not the newest one: [" + shouldBeTarget + "]. Re-pointing.";
                    LOG.warn(msg);
                    core.getActivityWriter().write(new Activity(msg, DeflectorManagerThread.class));

                    core.getDeflector().pointTo(shouldBeTarget, currentTarget);
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
