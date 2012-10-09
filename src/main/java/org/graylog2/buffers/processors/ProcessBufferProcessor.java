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

import com.lmax.disruptor.EventHandler;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.graylog2.Core;
import org.graylog2.buffers.LogMessageEvent;
import org.graylog2.logmessage.LogMessageImpl;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.logmessage.LogMessage;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ProcessBufferProcessor implements EventHandler<LogMessageEvent> {

    private static final Logger LOG = Logger.getLogger(ProcessBufferProcessor.class);
    protected static final Meter incomingMessagesMeter = Metrics.newMeter(ProcessBufferProcessor.class, "IncomingMessages", "messages", TimeUnit.SECONDS);
    protected static final Meter incomingMessagesMinutelyMeter = Metrics.newMeter(ProcessBufferProcessor.class, "IncomingMessagesMinutely", "messages", TimeUnit.MINUTES);
    protected static final Meter outgoingMessagesMeter = Metrics.newMeter(ProcessBufferProcessor.class, "OutgoingMessages", "messages", TimeUnit.SECONDS);
    protected static final Meter filteredOutMessagesMeter = Metrics.newMeter(ProcessBufferProcessor.class, "FilteredOutMessages", "messages", TimeUnit.SECONDS);
    protected static final Timer processTimer = Metrics.newTimer(ProcessBufferProcessor.class, "ProcessTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    private Core server;

    public ProcessBufferProcessor(Core server) {
        this.server = server;
    }

    @Override
    public void onEvent(LogMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        incomingMessagesMeter.mark();
        incomingMessagesMinutelyMeter.mark();
        TimerContext tcx = processTimer.time();

        LogMessage msg = event.getMessage();

        LOG.debug("Starting to process message <" + msg.getId() + ">.");

        for (MessageFilter filter : server.getFilters()) {
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Applying filter [" + filter.getClass().getSimpleName() +"] on message <" + msg.getId() + ">.");
                }

                filter.filter(msg, server);
                if (filter.discard()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Filter [" + filter.getClass().getSimpleName() + "] marked message <" + msg.getId() + "> to be discarded. Dropping message.");
                    }
                    filteredOutMessagesMeter.mark();
                    return;
                }
            } catch (Exception e) {
                LOG.error("Could not apply filter [" + filter.getClass().getSimpleName() +"] on message <" + msg.getId() +">: ", e);
            }
        }

        LOG.debug("Finished processing message. Writing to output buffer.");
        outgoingMessagesMeter.mark();
        server.getOutputBuffer().insert(msg);
        tcx.stop();
    }

}
