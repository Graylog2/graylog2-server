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

import org.graylog2.Log;
import org.graylog2.database.MongoBridge;
import org.graylog2.messagehandlers.common.MessageCounter;

/**
 * GraphWriterThread.java: Dec 6, 2010 1:37:41 AM
 *
 * Writes data needed to generate graphs in web interface to MongoDB.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GraphWriterThread extends Thread {

    public final static int INTERVAL = 60;

    /**
     * Start the thread. Runs forever.
     */
    @Override public void run() {
        while (true) {
            try {
                // Insert count.
                int count = MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS);
                MongoBridge m = new MongoBridge();
                m.writeGraphInformation(MessageCounter.ALL_HOSTS, count);

                // Reset the counter.
                MessageCounter.getInstance().reset(MessageCounter.ALL_HOSTS);
            } catch(Exception e) {
                Log.warn("Error in GraphWriterThread: " + e.toString());
            }

            try { sleep(GraphWriterThread.INTERVAL*1000); } catch(InterruptedException e) {}
        }
    }

}