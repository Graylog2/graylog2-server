/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

import org.apache.log4j.Logger;
import org.graylog2.Main;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.retention.MessageRetention;
import org.graylog2.settings.Setting;

import java.util.concurrent.TimeUnit;

/**
 * MessageRetentionThread.java: Nov 22, 2011 7:35:10 PM
 * <p/>
 * Removes messages from indexer that are older than specified retention time.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageRetentionThread implements Runnable {

    private static final Logger LOG = Logger.getLogger(MessageRetentionThread.class);
    
    private Indexer indexer;

    public MessageRetentionThread(Indexer indexer) {
        this.indexer = indexer;
    }

    @Override
    public void run() {
        try {
            /*
             * Fetching retentionTimeInDays dynamically and not passed to constructor
             * to be sure to always have the *current* setting from database - even when
             * used with scheduler.
             */
            int retentionTimeDays = Setting.getRetentionTimeInDays();

            LOG.info("Initialized message retention thread - cleaning all messages older than <" + retentionTimeDays + " days>.");

            if (MessageRetention.performCleanup(retentionTimeDays, indexer)) {
                MessageRetention.updateLastPerformedTime();
            }
        } catch (Exception e) {
            LOG.fatal("Error in MessageRetentionThread: " + e.getMessage(), e);
        } finally {
            scheduleNextRun();
        }
    }

    private void scheduleNextRun() {
        // Schedule next run in [current frequency setting from database] minutes.
        int when = Setting.getRetentionFrequencyInMinutes();
        Main.scheduler.schedule(new MessageRetentionThread(indexer), when, TimeUnit.MINUTES);
        LOG.info("Scheduled next run of MessageRetentionThread in <" + when + " minutes>.");
    }
}