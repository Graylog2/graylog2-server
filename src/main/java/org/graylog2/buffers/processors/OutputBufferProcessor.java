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
import com.yammer.metrics.core.TimerContext;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Histogram;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.buffers.LogMessageEvent;
import org.graylog2.logmessage.LogMessage;
import org.graylog2.outputs.MessageOutput;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBufferProcessor implements EventHandler<LogMessageEvent> {

    private static final Logger LOG = Logger.getLogger(OutputBufferProcessor.class);
    protected static final Meter incomingMessagesMeter = Metrics.newMeter(OutputBufferProcessor.class, "IncomingMessages", "messages", TimeUnit.SECONDS);
    protected static final Histogram batchSizeHistogram = Metrics.newHistogram(OutputBufferProcessor.class, "BatchSize");

    private GraylogServer server;

    private final ThreadLocal<List<LogMessage>> tlsBuffer = new ThreadLocal() {
        @Override protected List<LogMessage> initialValue() {
            return Lists.newArrayList();
        }
    };

    public OutputBufferProcessor(GraylogServer server) {
        this.server = server;
    }

    @Override
    public void onEvent(LogMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        incomingMessagesMeter.mark();

        LogMessage msg = event.getMessage();
        LOG.debug("Processing message <" + msg.getId() + "> from OutputBuffer.");

        List<LogMessage> buffer = tlsBuffer.get();
        buffer.add(msg);

        if (endOfBatch || buffer.size() >= server.getConfiguration().getOutputBatchSize()) {
            for (MessageOutput output : server.getOutputs()) {
                try {
                    LOG.debug("Writing message batch to [" + output.getClass().getSimpleName() + "]. Size <" + buffer.size() + ">");

                    batchSizeHistogram.update(buffer.size());
                    output.write(buffer, server);
                } catch (Exception e) {
                    LOG.error("Could not write message batch to output [" + output.getClass().getSimpleName() +"].", e);
                } finally {
                    buffer.clear();
                }
            }
        }

        LOG.debug("Wrote message <" + msg.getId() + "> to all outputs. Finished handling.");
    }

}
