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
import org.graylog2.filters.MessageFilter;
import org.graylog2.logmessage.LogMessage;

/**
 * ProcessBufferProcessor.java: 17.04.2012 16:19:19
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ProcessBufferProcessor implements EventHandler<LogMessageEvent> {

    private static final Logger LOG = Logger.getLogger(ProcessBufferProcessor.class);

    private GraylogServer server;

    public ProcessBufferProcessor(GraylogServer server) {
        this.server = server;
    }

    @Override
    public void onEvent(LogMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        LogMessage msg = event.getMessage();

        LOG.debug("Starting to process message <" + msg.getId() + ">.");

        for (Class filterType : server.getFilters()) {
            try {
                // Always create a new instance of this filter.
                MessageFilter filter = (MessageFilter) filterType.newInstance();
      
                String name = filterType.getSimpleName();
                LOG.debug("Applying filter [" + name +"] on message <" + msg.getId() + ">.");

                filter.filter(msg, server);

                if (filter.discardMessage()) {
                    LOG.debug("Filter [" + name + "] marked message <" + msg.getId() + "> to be discarded. Dropping message.");
                    return;
                }
            } catch (Exception e) {
                LOG.error("Could not apply filter [" + filterType.getSimpleName() +"] on message <" + msg.getId() +">: ", e);
            }
        }

        LOG.debug("Finished processing message. Writing to output buffer.");
        server.getOutputBuffer().insert(msg);
    }

}
