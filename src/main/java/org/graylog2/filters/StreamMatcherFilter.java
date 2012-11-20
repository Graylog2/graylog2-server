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
import org.graylog2.Core;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.logmessage.LogMessage;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamMatcherFilter implements MessageFilter {

    private static final Logger LOG = LoggerFactory.getLogger(StreamMatcherFilter.class);

    private static final StreamRouter ROUTER = new StreamRouter();
    private final Timer processTime = Metrics.newTimer(StreamMatcherFilter.class, "ProcessTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    @Override
    public boolean filter(LogMessage msg, GraylogServer server) {
        TimerContext tcx = processTime.time();

        List<Stream> streams = ROUTER.route((Core) server, msg);
        msg.setStreams(streams);

        LOG.debug("Routed message <{}> to {} streams.", msg.getId(), streams.size());

        tcx.stop();
        
        return false;
    }

    @Override
    public String getName() {
        return "StreamMatcher";
    }
    
}
