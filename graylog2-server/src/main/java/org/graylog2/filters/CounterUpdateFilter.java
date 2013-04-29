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

package org.graylog2.filters;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.MessageCounter;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;

import java.util.concurrent.TimeUnit;
import org.graylog2.Core;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class CounterUpdateFilter implements MessageFilter {

    private final Timer processTime = Metrics.newTimer(CounterUpdateFilter.class, "ProcessTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    @Override
    public boolean filter(Message msg, GraylogServer server) {
        Core serverImpl = (Core) server;
        TimerContext tcx = processTime.time();

        // Increment all registered message counters.
        for (MessageCounter counter : serverImpl.getMessageCounterManager().getAllCounters().values()) {
            // Five second throughput for health page.
            counter.incrementThroughput();

            // Total count.
            counter.incrementTotal();

            // Stream counts.
            for (Stream stream : msg.getStreams()) {
                counter.incrementStream(stream.getId());
            }

            // Host count.
            counter.incrementSource(msg.getSource());
        }

        tcx.stop();
        return false;
    }
    
    @Override
    public String getName() {
        return "CounterUpdater";
    }

}
