/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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
import org.graylog2.ServerValue;
import org.graylog2.messagehandlers.common.MessageCounter;

/**
 * SystemStatisticThread.java: Oct 25, 2010 6:36:05 PM
 *
 * Prints out load statistic information every second.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ThroughputWriterThread extends Thread {

    private static final Logger LOG = Logger.getLogger(ThroughputWriterThread.class);

    /**
     * Start the thread. Runs forever.
     */
    @Override public void run() {
        // Run forever.
        while (true) {
            try {
                MessageCounter counter = MessageCounter.getInstance();

                ServerValue.writeThroughput(counter.getTotalSecondCount(), counter.getHighestSecondCount());
                counter.resetTotalSecondCount();
            } catch (Exception e) {
                LOG.warn("Error in ThroughputWriterThread: " + e.getMessage(), e);
            }

           // Run every second.
           try { Thread.sleep(1000); } catch(InterruptedException e) {}
        }
    }

}
