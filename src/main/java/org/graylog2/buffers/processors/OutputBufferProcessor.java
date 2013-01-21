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

package org.graylog2.buffers.processors;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.graylog2.Core;
import org.graylog2.buffers.LogMessageEvent;
import org.graylog2.buffers.LogMessagesEvent;
import org.graylog2.outputs.ElasticSearchOutput;
import org.graylog2.outputs.OutputRouter;
import org.graylog2.outputs.OutputStreamConfigurationImpl;
import org.graylog2.plugin.logmessage.LogMessage;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.OutputStreamConfiguration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBufferProcessor implements EventHandler<LogMessagesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OutputBufferProcessor.class);

    private Core server;

    private final Meter incomingMessages = Metrics.newMeter(OutputBufferProcessor.class, "IncomingMessages", "messages", TimeUnit.SECONDS);
    
    private final Histogram batchSize = Metrics.newHistogram(OutputBufferProcessor.class, "BatchSize");

    private final long ordinal;
    private final long numberOfConsumers;
    
    public OutputBufferProcessor(Core server, final long ordinal, final long numberOfConsumers) {
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
        this.server = server;
        
    }

    @Override
    public void onEvent(LogMessagesEvent event, long sequence, boolean endOfBatch) throws Exception {
        // Because Trisha said so. (http://code.google.com/p/disruptor/wiki/FrequentlyAskedQuestions)
        if ((sequence % numberOfConsumers) != ordinal) {
            return;
        }
        
        server.outputBufferWatermark().decrementAndGet();
        final List<LogMessage> buffer = event.getAndResetMessages();
        incomingMessages.mark(buffer.size());

        LOG.debug("Processing messages batch of size <{}> from OutputBuffer.", buffer.size());

        for (final MessageOutput output : server.getOutputs()) {
            final String typeClass = output.getClass().getCanonicalName();
            // Always write to ElasticSearch, but only write to other outputs if enabled for one of its streams.
            if (output instanceof ElasticSearchOutput || OutputRouter.checkRouting(typeClass, buffer.get(0))) {
                try {
                    // We must copy the buffer for this output, because it may be cleared before all messages are handled.
                    LOG.debug("Writing message batch to [{}]. Size <{}>", output.getName(), buffer.size());

                    batchSize.update(buffer.size());

                    output.write(buffer, buildStreamConfigs(buffer, typeClass), server);

                } catch (Exception e) {
                    LOG.error("Could not write message batch to output [" + output.getName() +"].", e);
                }
            } 
        }

        LOG.debug("Wrote message batch of size <{}> to all outputs. Finished handling.", buffer.size());
    }

    private OutputStreamConfiguration buildStreamConfigs(List<LogMessage> messages, String className) {
        OutputStreamConfigurationImpl configs = new OutputStreamConfigurationImpl();
        
        for (LogMessage message : messages) {
            for (Stream stream : message.getStreams()) {
                if (!configs.exist(stream.getId())) {
                    configs.add(stream.getId(), ((StreamImpl)stream).getOutputConfigurations(className));
                }
                    
            }
        }
        
        return configs;
    }
    
}
