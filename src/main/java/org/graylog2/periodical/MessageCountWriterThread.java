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
import org.graylog2.database.MongoBridge;
import org.graylog2.messagehandlers.common.MessageCounter;


/**
 * MessageCountWriterThread.java: Sep 21, 2011 4:09:55 PM
 *
 * Periodically writes message counts to message count collection.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageCountWriterThread extends Thread {

    private static final Logger LOG = Logger.getLogger(MessageCountWriterThread.class);

    /**
     * Start the thread. Runs forever.
     */
    @Override public void run() {
        // Run forever.
        while (true) {
            // Run every 60 seconds.
            try { Thread.sleep(60000); } catch(InterruptedException e) {}

            MessageCounter counter = MessageCounter.getInstance();
            try {
                MongoBridge m = new MongoBridge();
                m.writeMessageCounts(counter.getTotalCount(), counter.getStreamCounts(), counter.getHostCounts());
            } catch (Exception e) {
                LOG.warn("Error in MessageCountWriterThread: " + e.getMessage(), e);
            } finally {
                counter.resetAllCounts();
            }
        }
    }

}
