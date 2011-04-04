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
import org.graylog2.HostSystem;
import org.graylog2.Tools;
import org.graylog2.database.MongoBridge;

/**
 * ServerValueWriterThread.java
 *
 * Periodically writes server values to MongoDB.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ServerValueWriterThread extends Thread {

    private static final Logger LOG = Logger.getLogger(ServerValueWriterThread.class);

    /**
     * Start the thread. Runs forever.
     */
    @Override public void run() {
        // Run forever.
        while (true) {
            try {
                HostSystem.writeSystemHealthHistorically();

                // Ping. (Server is running.)
                MongoBridge m = new MongoBridge();
                m.setSimpleServerValue("ping", Tools.getUTCTimestamp());

            } catch (Exception e) {
                LOG.warn("Error in SystemValueHistoryWriterThread: " + e.getMessage(), e);
            }
            
           // Run every 60 seconds.
           try { Thread.sleep(60000); } catch(InterruptedException e) {}
        }
    }

}
