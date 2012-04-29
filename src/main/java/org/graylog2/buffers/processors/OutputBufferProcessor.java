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
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.buffers.LogMessageEvent;
import org.graylog2.logmessage.LogMessage;
import org.graylog2.outputs.MessageOutput;

/**
 * OutputBufferProcessor.java: 29.04.2012 21:05:44
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBufferProcessor implements EventHandler<LogMessageEvent> {

    private static final Logger LOG = Logger.getLogger(OutputBufferProcessor.class);

    private GraylogServer server;

    public OutputBufferProcessor(GraylogServer server) {
        this.server = server;
    }

    @Override
    public void onEvent(LogMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        LogMessage msg = event.getMessage();

        LOG.debug("Processing message <" + msg.getId() + "> from OutputBuffer.");

        String originalMsgId = msg.getId();
        for (Class outputType : server.getOutputs()) {
            try {
                // Always create a new instance of this filter.
                MessageOutput output = (MessageOutput) outputType.newInstance();

                LOG.debug("Writing message <" + msg.getId() + "> to [" + outputType.getSimpleName() + "]");

                output.write(msg, server);
            } catch (Exception e) {
                LOG.error("Could not write message <" + msg.getId() +"> to output [" + outputType.getSimpleName() +"].", e);
            }
        }

        LOG.debug("Wrote message <" + msg.getId() + "> to all outputs. Finished handling.");
    }

}
