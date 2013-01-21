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

import com.google.common.collect.Lists;
import com.lmax.disruptor.EventHandler;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.Core;
import org.graylog2.buffers.LogMessageEvent;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.logmessage.LogMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ProcessBufferProcessor implements EventHandler<LogMessageEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessBufferProcessor.class);

    private Core server;
    private final Meter incomingMessages = Metrics.newMeter(ProcessBufferProcessor.class, "IncomingMessages", "messages", TimeUnit.SECONDS);
    private final Meter incomingMessagesPerMinute = Metrics.newMeter(ProcessBufferProcessor.class, "IncomingMessagesMinutely", "messages", TimeUnit.MINUTES);
    private final Timer processTime = Metrics.newTimer(ProcessBufferProcessor.class, "ProcessTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);
    private final Meter filteredOutMessages = Metrics.newMeter(ProcessBufferProcessor.class, "FilteredOutMessages", "messages", TimeUnit.SECONDS);
    private final Meter outgoingMessages = Metrics.newMeter(ProcessBufferProcessor.class, "OutgoingMessages", "messages", TimeUnit.SECONDS);

    private final long ordinal;
    private final long numberOfConsumers;
    private List<LogMessage> buffer;

    private final int bufferSize;
    
    public ProcessBufferProcessor(Core server, final long ordinal, final long numberOfConsumers) {
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
        this.server = server;
        this.bufferSize = server.getConfiguration().getOutputBatchSize();
        this.buffer = new ArrayList<LogMessage>(bufferSize);
    }

    @Override
    public void onEvent(LogMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        // Because Trisha said so. (http://code.google.com/p/disruptor/wiki/FrequentlyAskedQuestions)
        if ((sequence % numberOfConsumers) != ordinal) {
            return;
        }
        
        server.processBufferWatermark().decrementAndGet();
        
        incomingMessages.mark();
        incomingMessagesPerMinute.mark();
        TimerContext tcx = processTime.time();

        LogMessage msg = event.getAndResetMessage();

        LOG.debug("Starting to process message <{}>.", msg.getId());

        for (MessageFilter filter : server.getFilters()) {
            try {
                LOG.debug("Applying filter [{}] on message <{}>.", filter.getName(), msg.getId());

                if (filter.filter(msg, server)) {
                    LOG.debug("Filter [{}] marked message <{}> to be discarded. Dropping message.", filter.getName(), msg.getId());
                    filteredOutMessages.mark();
                    return;
                }
            } catch (Exception e) {
                LOG.error("Could not apply filter [" + filter.getName() +"] on message <" + msg.getId() +">: ", e);
            }
        }

        LOG.debug("Finished processing message. Writing to output buffer.");
        
        this.buffer.add(msg);

        // TODO add time limit as well
        while (this.buffer.size()>=this.bufferSize) {
            try {
               ((OutputBuffer) server.getOutputBuffer()).insert(this.buffer);
               this.buffer = new ArrayList<LogMessage>(this.bufferSize);
               break;
            } catch (BufferOutOfCapacityException e) {
                LOG.debug("OutputBuffer out of capacity. Trying again in 250ms.");
                Thread.sleep(250);
                continue;
            }
        }
        
        outgoingMessages.mark();
        tcx.stop();
    }

}
