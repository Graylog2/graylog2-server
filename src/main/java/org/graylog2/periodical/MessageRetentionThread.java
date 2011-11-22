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

import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.graylog2.Main;
import org.graylog2.indexer.retention.MessageRetention;
import org.graylog2.settings.Setting;

/**
 * MessageRetentionThread.java: Nov 22, 2011 7:35:10 PM
 * <p/>
 * Removes messages from indexer that are older than specified retention time.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageRetentionThread implements Runnable {

    private static final Logger LOG = Logger.getLogger(MessageRetentionThread.class);

    private int retentionTimeDays;

    @Override
    public void run() {
        try {
            /*
             * Fetching retentionTimeInDays dynamically and not passed to constructor
             * to be sure to always have the *current* setting from database - even when
             * used with scheduler.
             */
            this.retentionTimeDays = Setting.getRetentionTimeInDays();

            LOG.info("Initialized message retention thread - cleaning all messages older than <" + this.retentionTimeDays + " days>.");

            MessageRetention.performCleanup(this.retentionTimeDays);
            MessageRetention.updateLastPerformedTime();
        } catch (Exception e) {
            LOG.fatal("Error in MessageRetentionThread: " + e.getMessage(), e);
        }

        scheduleNextRun();
    }

    private void scheduleNextRun() {
        // Schedule next run in [current frequency setting from database] minutes.
        int when = Setting.getRetentionFrequencyInMinutes();
        Main.scheduler.schedule(new MessageRetentionThread(), when, TimeUnit.MINUTES);
        LOG.info("Scheduled next run of MessageRetentionThread in <" + when + " minutes>.");
    }
}