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

package org.graylog2.inputs.syslog;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.TimerContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.Tools;
import org.graylog2.logmessage.LogMessage;
import org.productivity.java.syslog4j.server.impl.event.SyslogServerEvent;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogProcessor {

    private static final Logger LOG = Logger.getLogger(SyslogProcessor.class);
    private GraylogServer server;

    public SyslogProcessor(GraylogServer server) {
        this.server = server;
    }

    public void messageReceived(String msg, InetAddress remoteAddress) throws Exception {
        Metrics.newMeter(SyslogProcessor.class, "IncomingMessages", "messages", TimeUnit.SECONDS).mark();

        // Convert to LogMessage
        LogMessage lm;
        try {
            lm = parse(msg, remoteAddress);
        } catch (Exception e) {
            Metrics.newMeter(SyslogProcessor.class, "MessageParsingFailures", "failures", TimeUnit.SECONDS).mark();
            LOG.error("Could not parse syslog message. Not further handling.", e);
            return;
        }

        if (!lm.isComplete()) {
            Metrics.newMeter(SyslogProcessor.class, "IncompleteMessages", "messages", TimeUnit.SECONDS).mark();
            LOG.debug("Skipping incomplete message.");
            return;
        }
        
        // Possibly remove full message.
        if (!this.server.getConfiguration().isSyslogStoreFullMessageEnabled()) {
            lm.setFullMessage(null);
        }

        // Add to process buffer.
        LOG.debug("Adding received syslog message <" + lm.getId() +"> to process buffer: " + lm);
        Metrics.newMeter(SyslogProcessor.class, "ProcessedMessages", "messages", TimeUnit.SECONDS).mark();
        server.getProcessBuffer().insert(lm);
    }

    private LogMessage parse(String msg, InetAddress remoteAddress) throws UnknownHostException {
        TimerContext tcx = Metrics.newTimer(SyslogProcessor.class, "SyslogParsedTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS).time();

        LogMessage lm = new LogMessage();

        SyslogServerEvent e = new SyslogServerEvent(msg, remoteAddress);

        lm.setShortMessage(e.getMessage());
        lm.setHost(parseHost(e, remoteAddress));
        lm.setFacility(Tools.syslogFacilityToReadable(e.getFacility()));
        lm.setLevel(e.getLevel());
        lm.setFullMessage(new String(e.getRaw()));
        lm.setCreatedAt(parseDate(e));
        lm.addAdditionalData(parseAdditionalData(e));

        tcx.stop();

        return lm;
    }

    private Map<String, String> parseAdditionalData(SyslogServerEvent msg) {
        // Parse possibly included structured syslog data into additional_fields.
        Map<String, String> structuredData = StructuredSyslog.extractFields(msg.getRaw());

        if (structuredData.size() > 0) {
            LOG.debug("Parsed <" + structuredData.size() + "> structured data pairs."
                        + " Adding as additional_fields. Not using tokenizer.");
        }

        return structuredData;
    }

    private String parseHost(SyslogServerEvent msg, InetAddress remoteAddress) {
        if (server.getConfiguration().getForceSyslogRdns()) {
            try {
                return Tools.rdnsLookup(remoteAddress);
            } catch (UnknownHostException e) {
                LOG.warn("Reverse DNS lookup failed. Falling back to parsed hostname.", e);
            }
        }

        return msg.getHost();
    }

    private double parseDate(SyslogServerEvent msg) throws IllegalStateException {
        // Check if date could be parsed.
        if (msg.getDate() == null) {
            if (server.getConfiguration().getAllowOverrideSyslogDate()) {
                // empty Date constructor allocates a Date object and initializes it so that it represents the time at which it was allocated.
                msg.setDate(new Date());
                LOG.info("Date could not be parsed. Was set to NOW because allow_override_syslog_date is true.");
            } else {
                LOG.info("Syslog message is missing date or date could not be parsed. (Possibly set allow_override_syslog_date to true) "
                        + "Not further handling. Message was: " + new String(msg.getRaw()));
                throw new IllegalStateException();
            }
        }

        return Tools.getUTCTimestampWithMilliseconds(msg.getDate().getTime());
    }

}
