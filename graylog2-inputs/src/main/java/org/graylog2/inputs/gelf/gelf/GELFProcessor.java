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
package org.graylog2.inputs.gelf.gelf;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GELFProcessor.class);
    private final Buffer processBuffer;

    private MetricRegistry metricRegistry;

    private final GELFParser gelfParser;

    public GELFProcessor(MetricRegistry metricRegistry, Buffer processBuffer) {
        this(metricRegistry, processBuffer, new GELFParser(metricRegistry));
    }

    public GELFProcessor(MetricRegistry metricRegistry, Buffer processBuffer, GELFParser gelfParser) {
        this.processBuffer = processBuffer;
        this.metricRegistry = metricRegistry;
        this.gelfParser = gelfParser;
    }

    public void messageReceived(GELFMessage message, MessageInput sourceInput) throws BufferOutOfCapacityException {
        Message lm = prepareMessage(message, sourceInput);
        if (lm == null) return;
        processBuffer.insertCached(lm, sourceInput);
    }

    public void messageReceivedFailFast(GELFMessage message, MessageInput sourceInput) throws BufferOutOfCapacityException, ProcessingDisabledException {
        Message lm = prepareMessage(message, sourceInput);
        if (lm == null) return;
        processBuffer.insertFailFast(lm, sourceInput);
    }

    private Message prepareMessage(GELFMessage message, MessageInput sourceInput) {
        String metricName = sourceInput.getUniqueReadableId();

        metricRegistry.meter(name(metricName, "incomingMessages")).mark();

        // Convert to LogMessage
        Message lm = null;
        try {
            lm = gelfParser.parse(message.getJSON(), sourceInput);
        } catch (IllegalStateException e) {
            LOG.error("Corrupt or invalid message received: ", e);
            return null;
        }

        if (lm == null || !lm.isComplete()) {
            metricRegistry.meter(name(metricName, "incompleteMessages")).mark();
            LOG.debug("Skipping incomplete message: {}", lm.getValidationErrors());
            return null;
        }

        // Add to process buffer.
        LOG.debug("Adding received GELF message <{}> to process buffer: {}", lm.getId(), lm);
        metricRegistry.meter(name(metricName, "processedMessages")).mark();
        return lm;
    }
}
