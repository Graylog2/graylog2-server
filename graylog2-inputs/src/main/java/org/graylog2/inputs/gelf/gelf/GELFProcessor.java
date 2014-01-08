/**
 * Copyright 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.inputs.gelf.gelf;

import com.fasterxml.jackson.core.JsonParser;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFProcessor extends GELFParser {

    private static final Logger LOG = LoggerFactory.getLogger(GELFProcessor.class);

    public GELFProcessor(InputHost server) {
        super(server);

        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    public void messageReceived(GELFMessage message, MessageInput sourceInput) throws BufferOutOfCapacityException {
        String metricName = sourceInput.getUniqueReadableId();

        server.metrics().meter(name(metricName, "incomingMessages")).mark();

        // Convert to LogMessage
        Message lm = parse(message.getJSON(), sourceInput);

        if (!lm.isComplete()) {
            server.metrics().meter(name(metricName, "incompleteMessages")).mark();
            LOG.debug("Skipping incomplete message.");
            return;
        }

        // Add to process buffer.
        LOG.debug("Adding received GELF message <{}> to process buffer: {}", lm.getId(), lm);
        server.metrics().meter(name(metricName, "processedMessages")).mark();
        server.getProcessBuffer().insertCached(lm, sourceInput);
    }

}
