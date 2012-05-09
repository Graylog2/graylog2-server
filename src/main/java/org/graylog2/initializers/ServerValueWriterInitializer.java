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

package org.graylog2.initializers;

import org.graylog2.Configuration;
import org.graylog2.GraylogServer;
import org.graylog2.HostSystem;
import org.graylog2.ServerValue;
import org.graylog2.Tools;
import org.graylog2.periodical.ServerValueWriterThread;

/**
 * ServerValueWriterInitializer.java: Apr 11, 2012 7:28:52 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ServerValueWriterInitializer extends SimpleFixedRateScheduleInitializer implements Initializer {

    private Configuration configuration;

    public ServerValueWriterInitializer(GraylogServer graylogServer, Configuration configuration) {
        this.graylogServer = graylogServer;
        this.configuration = configuration;
    }

    @Override
    public void initialize() {
        // Write some initial values. Only done once.
        ServerValue serverValue = graylogServer.getServerValues();
        serverValue.setStartupTime(Tools.getUTCTimestamp());
        serverValue.setPID(Integer.parseInt(Tools.getPID()));
        serverValue.setJREInfo(Tools.getSystemInformation());
        serverValue.setGraylog2Version(GraylogServer.GRAYLOG2_VERSION);
        serverValue.setAvailableProcessors(HostSystem.getAvailableProcessors());
        serverValue.setLocalHostname(Tools.getLocalHostname());

        configureScheduler(
                new ServerValueWriterThread(graylogServer),
                ServerValueWriterThread.INITIAL_DELAY,
                ServerValueWriterThread.PERIOD
        );
    }

}