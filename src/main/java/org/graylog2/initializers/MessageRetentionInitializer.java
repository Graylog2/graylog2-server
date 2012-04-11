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

import java.util.concurrent.TimeUnit;
import org.graylog2.GraylogServer;
import org.graylog2.periodical.MessageRetentionThread;

/**
 * MessageCounterInitializer.java: Apr 11, 2012 7:36:25 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageRetentionInitializer implements Initializer {

    GraylogServer graylogServer;

    /*
     * Beware! This is not a fixed rate scheduled thread. Called only once.
     * Look into MessageRetentionThread#scheduleNextRun().
     */
    public MessageRetentionInitializer(GraylogServer graylogServer) {
        this.graylogServer = graylogServer;
    }

    @Override
    public void initialize() {
        this.graylogServer.getScheduler().schedule(new MessageRetentionThread(graylogServer),0,TimeUnit.SECONDS);
    }
    
}