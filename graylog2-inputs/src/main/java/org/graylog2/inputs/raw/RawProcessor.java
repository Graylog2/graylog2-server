/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.inputs.raw;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RawProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RawProcessor.class);
    private final InputHost server;
    private final Configuration config;

    private final MessageInput sourceInput;

    private final Meter incomingMessages;
    private final Meter failures;
    private final Meter incompleteMessages;
    private final Meter processedMessages;
    private final Timer parseTime;

    public RawProcessor(InputHost server, Configuration config, MessageInput sourceInput) {
        this.server = server;
        this.config = config;

        this.sourceInput = sourceInput;

        String metricName = sourceInput.getUniqueReadableId();
        this.incomingMessages = server.metrics().meter(name(metricName, "incomingMessages"));
        this.failures = server.metrics().meter(name(metricName, "failures"));
        this.processedMessages = server.metrics().meter(name(metricName, "processedMessages"));
        this.incompleteMessages = server.metrics().meter(name(metricName, "incompleteMessages"));
        this.parseTime = server.metrics().timer(name(metricName, "parseTime"));
    }

    public void messageReceived(String msg, InetAddress remoteAddress) throws BufferOutOfCapacityException {
        incomingMessages.mark();

        // Convert to LogMessage
        Message lm;
        try {
            lm = new Message(msg, parseSource(msg, remoteAddress), new DateTime());
        } catch (Exception e) {
            failures.mark();
            LOG.error("Could not parse raw message. Not further handling.", e);
            return;
        }

        if (!lm.isComplete()) {
            incompleteMessages.mark();
            LOG.debug("Skipping incomplete message.");
            return;
        }

        // Add to process buffer.
        LOG.debug("Adding received raw message <{}> to process buffer: {}", lm.getId(), lm);
        processedMessages.mark();
        server.getProcessBuffer().insertCached(lm, sourceInput);
    }

    private String parseSource(String msg, InetAddress remoteAddress) {
        if (config.stringIsSet(RawInputBase.CK_OVERRIDE_SOURCE)) {
            return config.getString(RawInputBase.CK_OVERRIDE_SOURCE);
        }

        return remoteAddress.getCanonicalHostName();
    }

}
