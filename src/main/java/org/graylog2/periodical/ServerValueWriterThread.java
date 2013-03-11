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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.Core;
import org.graylog2.plugin.MessageCounter;

/**
 * Periodically writes server values to MongoDB.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ServerValueWriterThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ServerValueWriterThread.class);

    public static final int PERIOD = 5;
    public static final int INITIAL_DELAY = 0;

    private final Core graylogServer;

    public ServerValueWriterThread(Core graylogServer) {
        this.graylogServer = graylogServer;
    }

    /**
     * Start the thread. Runs forever.
     */
    @Override
    public void run() {
        try {
            // ohai, we are alive. \o/
            graylogServer.getServerValues().ping();

            // Current throughput.
            MessageCounter c = this.graylogServer.getMessageCounterManager().get(Core.MASTER_COUNTER_NAME);
            graylogServer.getServerValues().writeThroughput(c.getThroughput(), c.getHighestThroughput());
            c.resetThroughput(); // Reset five second throughput count.
        } catch (Exception e) {
            LOG.warn("Error in ServerValue  WriterThread: " + e.getMessage(), e);
        }
    }
}
