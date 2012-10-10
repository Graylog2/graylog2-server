/**
 * Copyright 2011 Rackspace Hosting Inc.
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

package org.graylog2.inputs.scribe;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.TimerContext;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.inputs.gelf.GELFMessage;
import org.graylog2.inputs.gelf.GELFProcessor;
import scribe.LogEntry;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ScribeBatchHandler implements Runnable {
    private final GraylogServer graylogServer;
    private final List<LogEntry> scribeBatch;
    private final AtomicInteger counter;

    private static int GELF_MESSAGE_HEADER_SIZE = 2;
    private static final Logger LOG = Logger.getLogger(ScribeBatchHandler.class);

    public ScribeBatchHandler(GraylogServer graylogServer, List<LogEntry> scribeBatch, AtomicInteger counter) {
        this.graylogServer = graylogServer;
        this.scribeBatch = scribeBatch;
        this.counter = counter;
    }

    @Override
    public void run() {
        TimerContext tcx = Metrics.newTimer(ScribeBatchHandler.class, "ScribeBatchProcessingTime",
                TimeUnit.MILLISECONDS, TimeUnit.SECONDS).time();
        Metrics.newHistogram(ScribeBatchHandler.class, "BatchSize").update(scribeBatch.size());
        for (LogEntry entry : scribeBatch) {
            LOG.trace("Received new scribe message: category= " + entry.category + " message= " + entry.message);
            try {
                handleMessage(entry.message);
            } catch (Exception ex) {
                LOG.error("Failed to process message: category= " + entry.category + " message= " + entry.message, ex);
                // We'd still continue.
            }
        }
        counter.decrementAndGet();
        tcx.stop();
    }

    private void handleMessage(String msgBody) throws IOException {
        final GELFMessage message = new GELFMessage(getGELFPayload(msgBody));
        if (message.getGELFType() != GELFMessage.Type.UNCOMPRESSED) {
            LOG.error("Cannot create GELFMessage from message body");
            throw new IOException("Cannot create GELFMessage from message body");
        }

        // Handle GELF message.
        GELFProcessor gelfProcessor = new GELFProcessor(graylogServer);
        gelfProcessor.messageReceived(message);
    }

    byte[] getGELFPayload(String msgBody) {
        // Convert string payload to GELF message
        final byte[] payload = new byte[msgBody.length() + GELF_MESSAGE_HEADER_SIZE];
        payload[0] = (byte) 0x1f;    // magic headers
        payload[1] = (byte) 0x3c;
        System.arraycopy(msgBody.getBytes(), 0, payload, 2, msgBody.length());

        return payload;
    }
}