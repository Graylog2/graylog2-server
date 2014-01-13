/*
 * Copyright 2013 TORCH GmbH
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
 */
package org.graylog2.periodical;

import org.cliffc.high_scale_lib.Counter;
import org.graylog2.Core;

import java.util.HashMap;
import java.util.Map;

public class StreamThroughputCounterManagerThread implements Runnable {

    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 1;

    private final Core server;

    public StreamThroughputCounterManagerThread(Core server) {
        this.server = server;
    }

    @Override
    public void run() {
        // cycleStreamThroughput clears the map already.
        final Map<String,Counter> stringCounterMap = server.cycleStreamThroughput();
        server.setCurrentStreamThroughput(new HashMap<String, Counter>(stringCounterMap));
    }
}
