/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.messageprocessors;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public class StreamMatcherProcessor implements MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(StreamMatcherProcessor.class);

    public static class Descriptor implements MessageProcessor.Descriptor {
        @Override
        public String name() {
            return "Stream Matcher Processor";
        }

        @Override
        public String className() {
            return StreamMatcherProcessor.class.getCanonicalName();
        }
    }

    private final StreamRouter streamRouter;

    @Inject
    public StreamMatcherProcessor(StreamRouter streamRouter) {
        this.streamRouter = streamRouter;
    }

    @Override
    public Messages process(Messages messages) {
        for (Message message : messages) {
            final List<Stream> streams = streamRouter.route(message);
            message.addStreams(streams);

            LOG.debug("Routed message <{}> to {} streams.", message.getId(), streams.size());
        }

        return messages;
    }
}
