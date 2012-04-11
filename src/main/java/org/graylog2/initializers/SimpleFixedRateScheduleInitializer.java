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

/**
 * SimpleFixedRateScheduleInitializer.java: Apr 11, 2012 6:03:32 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SimpleFixedRateScheduleInitializer {

    protected SimpleFixedRateScheduleInitializer() { /* Nope. No instances desired. Only to be inherited by Initializers if wanted. */ }

    protected GraylogServer graylogServer = null;

    public void configureScheduler(Runnable what, int initialDelay, int period) {
        this.graylogServer.getScheduler()
                .scheduleAtFixedRate(
                    what,
                    initialDelay,
                    period,
                    TimeUnit.SECONDS
                );
    }
    
}