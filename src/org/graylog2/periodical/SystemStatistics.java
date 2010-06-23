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
 * SystemStatistics.java: Lennart Koopmann <lennart@scopeport.org> | May 21, 2010 8:12:56 PM
 */

package org.graylog2.periodical;

import org.graylog2.Main;
import org.graylog2.database.MongoBridge;

public final class SystemStatistics {
    private static SystemStatistics INSTANCE;

    private SystemStatistics() {}

    public synchronized static SystemStatistics getInstance() {
      if (INSTANCE == null) {
            INSTANCE = new SystemStatistics();
        }
        return INSTANCE;
    }

    public void clearCollection() throws Exception {
        MongoBridge m = new MongoBridge(
                Main.masterConfig.getProperty("mongodb_user"),
                Main.masterConfig.getProperty("mongodb_password"),
                Main.masterConfig.getProperty("mongodb_host"),
                Main.masterConfig.getProperty("mongodb_database"),
                Integer.valueOf(Main.masterConfig.getProperty("mongodb_port"))
        );

        m.dropCollection("systemstatistics");
    }

    //// Handled syslog events ////
    private long handledSyslogEvents = 0;

    public void countUpHandledSyslogEvents() {
        handledSyslogEvents += 1;
    }

    public long getHandledSyslogEvents() {
        return handledSyslogEvents;
    }
    ///////////////////////////////

}
