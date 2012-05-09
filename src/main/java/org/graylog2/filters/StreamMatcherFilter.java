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

import java.util.List;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.logmessage.LogMessage;
import org.graylog2.streams.Stream;
import org.graylog2.streams.StreamRouter;

/**
 * StreamMatcherFilter.java: 19.04.2012 14:41:36
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamMatcherFilter implements MessageFilter {

    private static final Logger LOG = Logger.getLogger(StreamMatcherFilter.class);

    private static final StreamRouter ROUTER = new StreamRouter();

    @Override
    public boolean filter(LogMessage msg, GraylogServer server) {
        List<Stream> streams = ROUTER.route(msg);
        msg.setStreams(streams);

        LOG.debug("Routed message <" + msg.getId() + "> to " + streams.size() + " streams.");

        // Do not discard message.
        return false;
    }

}
