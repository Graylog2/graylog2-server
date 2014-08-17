/**
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
package org.graylog2.filters;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.stats.ThroughputStats;
import org.graylog2.streams.CachedStreamRouter;
import org.graylog2.streams.StreamRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamMatcherFilter implements MessageFilter {

    private static final Logger LOG = LoggerFactory.getLogger(StreamMatcherFilter.class);

    private final StreamRouter streamRouter;
    private final ThroughputStats throughputStats;

    @Inject
    public StreamMatcherFilter(CachedStreamRouter streamRouter,
                               ThroughputStats throughputStats) {
        this.streamRouter = streamRouter;
        this.throughputStats = throughputStats;
    }

    @Override
    public boolean filter(Message msg) {
        List<Stream> streams = streamRouter.route(msg);

        for (Stream stream : streams) {
            throughputStats.incrementStreamThroughput(stream.getId());
        }
        msg.setStreams(streams);

        LOG.debug("Routed message <{}> to {} streams.", msg.getId(), streams.size());

        return false;
    }

    @Override
    public String getName() {
        return "StreamMatcher";
    }

    @Override
    public int getPriority() {
        // of the built-in filters this gets run last
        return 40;
    }

}
