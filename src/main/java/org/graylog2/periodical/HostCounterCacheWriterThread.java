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

import org.graylog2.Log;
import org.graylog2.database.HostCounterCache;
import org.graylog2.database.MongoBridge;


/**
 * HostCounterCacheWriterThread.java: Feb 23, 2011 5:59:58 PM
 *
 * Periodically writes host counter cache to hosts collection.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class HostCounterCacheWriterThread extends Thread {

    /**
     * Start the thread. Runs forever.
     */
    @Override public void run() {
        // Run forever.
        while (true) {
            try {
                MongoBridge m = new MongoBridge();
                for (String host : HostCounterCache.getInstance().getAllHosts()) {
                    m.upsertHostCount(host, HostCounterCache.getInstance().getCount(host));
                    HostCounterCache.getInstance().reset(host);
                }
            } catch (Exception e) {
                Log.warn("Error in HostCounterCacheWriterThread: " + e.toString());
            }
            
           // Run every 5 seconds.
           try { Thread.sleep(5000); } catch(InterruptedException e) {}
        }
    }

}
