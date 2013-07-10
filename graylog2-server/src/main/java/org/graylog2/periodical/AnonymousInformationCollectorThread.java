/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
import org.graylog2.SystemSettingAccessor;
import org.graylog2.system.information.AnonymousInformationCollector;
import org.graylog2.system.information.Sender;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AnonymousInformationCollectorThread implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(AnonymousInformationCollectorThread.class);

    public static final int INITIAL_DELAY = 15;
    public static final int PERIOD = 3600; // Run every hour.
    
    private final Core graylogServer;
    
    public AnonymousInformationCollectorThread(Core graylogServer) {
        this.graylogServer = graylogServer;
    }

    @Override
    public void run() {
        SystemSettingAccessor s = new SystemSettingAccessor(graylogServer);
        if (s.allowUsageStats()) {
            LOG.debug("Collecting and sending anonymous usage information. "
                    + "Enabled and allowed in web interface settings.");
            AnonymousInformationCollector collector = new AnonymousInformationCollector(graylogServer);
            Sender.send(collector.collect(), graylogServer);
        }
    }
    
}
