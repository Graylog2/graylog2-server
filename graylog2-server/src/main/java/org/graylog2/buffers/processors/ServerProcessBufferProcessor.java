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
package org.graylog2.buffers.processors;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.messageprocessors.OrderedMessageProcessors;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerProcessBufferProcessor extends ProcessBufferProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ServerProcessBufferProcessor.class);

    private final OrderedMessageProcessors orderedMessageProcessors;
    private final OutputBuffer outputBuffer;

    @Inject
    public ServerProcessBufferProcessor(MetricRegistry metricRegistry,
                                        OrderedMessageProcessors orderedMessageProcessors,
                                        OutputBuffer outputBuffer) {
        super(metricRegistry);
        this.orderedMessageProcessors = orderedMessageProcessors;
        this.outputBuffer = outputBuffer;
    }

    @Override
    protected void handleMessage(@Nonnull Message msg) {
        Messages messages = msg;

        for (MessageProcessor messageProcessor : orderedMessageProcessors) {
            messages = messageProcessor.process(messages);
        }
        for (Message message : messages) {
            LOG.debug("Finished processing message. Writing to output buffer.");
            outputBuffer.insertBlocking(message);
        }
    }

}
