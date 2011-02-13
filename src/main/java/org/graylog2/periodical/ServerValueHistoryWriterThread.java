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

import org.graylog2.HostSystem;
import org.graylog2.Log;

/**
 * SystemStatisticThread.java: Oct 25, 2010 6:36:05 PM
 *
 * Prints out load statistic information every second.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ServerValueHistoryWriterThread extends Thread {

    /**
     * Start the thread. Runs forever.
     */
    @Override public void run() {
        // Run forever.
        while (true) {
            try {
                HostSystem.writeSystemHealthHistorically();
            } catch (Exception e) {
                Log.warn("Error in SystemValueHistoryWriterThread: " + e.toString());
            }
            
           // Run every 60 seconds.
           try { Thread.sleep(60000); } catch(InterruptedException e) {}
        }
    }

}
