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

import java.net.InetSocketAddress;
import org.graylog2.Configuration;
import org.graylog2.GraylogServer;
import org.graylog2.messagehandlers.gelf.ChunkedGELFClientManager;
import org.graylog2.messagehandlers.gelf.GELFMainThread;
import org.graylog2.periodical.ChunkedGELFClientManagerThread;

/**
 * GELFInitializer.java: 11.04.2012 19:06:34
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFInitializer extends SimpleFixedRateScheduleInitializer implements Initializer {

    private Configuration configuration;

    public GELFInitializer(GraylogServer graylogServer, Configuration configuration) {
        this.graylogServer = graylogServer;
        this.configuration = configuration;
    }

    @Override
    public void initialize() {
        new GELFMainThread(graylogServer, new InetSocketAddress(
                configuration.getGelfListenAddress(),
                configuration.getGelfListenPort())
        ).start();

        // Regularly call thead that handles GELF chunks from chunked messages.
        configureScheduler(
                new ChunkedGELFClientManagerThread(ChunkedGELFClientManager.getInstance()),
                ChunkedGELFClientManagerThread.INITIAL_DELAY,
                ChunkedGELFClientManagerThread.PERIOD
        );
    }

}