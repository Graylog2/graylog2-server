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
import org.graylog2.graphing.RRD;
import org.graylog2.messagehandlers.common.MessageCounter;

/**
 * RRDThread.java: Aug 19, 2010 6:10:11 PM
 *
 * Writes RRDs
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class RRDThread extends Thread {

    /**
     * Start the thread. Runs forever.
     */
    @Override public void run() {
        while (true) {
            try {
                // Write to RRD.
                RRD rrd = new RRD(RRD.GRAPH_TYPE_TOTAL);

                // Write the value.
                if (!rrd.write(MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS))) {
                    Log.crit("Could not write to RRD. Possibly not writable.");
                }

                // Reset the counter.
                MessageCounter.getInstance().reset(MessageCounter.ALL_HOSTS);
            } catch(Exception e) {
                Log.warn("Error in RRDThread: " + e.toString());
            }

            try { sleep(RRD.INTERVAL*1000); } catch(InterruptedException e) {}
        }
    }

}