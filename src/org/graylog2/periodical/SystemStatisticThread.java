/**
 * Copyright 2010 Lennart Koopmann <lennart@scopeport.org>
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

/**
 * SystemStatisticThread.java: Lennart Koopmann <lennart@scopeport.org> | May 21, 2010 6:42:25 PM
 */

package org.graylog2.periodical;

import org.graylog2.Log;
import org.graylog2.database.MongoBridge;

public class SystemStatisticThread extends Thread {

    @Override public void run() {
        // Run forever.
        while (true) {
            // Run every minute.
            try { Thread.sleep(60000); } catch(InterruptedException e) {}

            try {
                MongoBridge m = new MongoBridge();

                // Handled syslog events.
                m.insertSystemStatisticValue("handled_syslog_events", SystemStatistics.getInstance().getHandledSyslogEvents());
            } catch (Exception e) {
                Log.warn("Error in SystemStatisticThread: " + e.toString());
                continue;
            }
        }
    }

}
