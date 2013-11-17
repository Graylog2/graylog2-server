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

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Message;
import org.productivity.java.syslog4j.server.impl.event.SyslogServerEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Pattern;
import com.google.common.collect.Maps;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SyslogProcessor.class);
    private final InputHost server;
    private final Configuration config;

    private final MessageInput sourceInput;

    private static final Pattern STRUCTURED_SYSLOG_PATTERN = Pattern.compile("<\\d+>\\d.*", Pattern.DOTALL);
    
    private final Meter incomingMessages;
    private final Meter parsingFailures;
    private final Meter incompleteMessages;
    private final Meter processedMessages;
    private final Timer syslogParsedTime;

    public SyslogProcessor(InputHost server, Configuration config, MessageInput sourceInput) {
        this.server = server;
        this.config = config;

        this.sourceInput = sourceInput;

        String metricsId = sourceInput.getUniqueReadableId();
        this.incomingMessages = server.metrics().meter(name(metricsId, "incomingMessages"));
        this.parsingFailures = server.metrics().meter(name(metricsId, "parsingFailures"));
        this.processedMessages = server.metrics().meter(name(metricsId, "processedMessages"));
        this.incompleteMessages = server.metrics().meter(name(metricsId, "incompleteMessages"));
        this.syslogParsedTime = server.metrics().timer(name(metricsId, "syslogParsedTime"));
    }

    public void messageReceived(String msg, InetAddress remoteAddress) throws BufferOutOfCapacityException {
        incomingMessages.mark();

        // Convert to LogMessage
        Message lm;
        try {
            lm = parse(msg, remoteAddress);
        } catch (Exception e) {
            parsingFailures.mark();
            LOG.error("Could not parse syslog message. Not further handling.", e);
            return;
        }

        if (!lm.isComplete()) {
            incompleteMessages.mark();
            LOG.debug("Skipping incomplete message.");
            return;
        }

        // Add to process buffer.
        LOG.debug("Adding received syslog message <{}> to process buffer: {}", lm.getId(), lm);
        processedMessages.mark();
        server.getProcessBuffer().insertCached(lm, sourceInput);
    }

    private Message parse(String msg, InetAddress remoteAddress) throws UnknownHostException {
        Timer.Context tcx = syslogParsedTime.time();
        
        if (remoteAddress == null) {
            remoteAddress = InetAddress.getLocalHost();
        }

        /* 
         * ZOMG funny 80s neckbeard protocols. We are now deciding if to parse
         * structured (RFC5424) or unstructured (classic BSD, RFC3164) syslog
         * by checking if there is a VERSION after the PRI. Sorry.
         * 
         *                            ._.                                  _
         *    R-O-F-L-R-O-F-L-R-O-F-L-IOI-R-O-F-L-R-O-F-L-R-O-F-L         / l
         *                ___________/LOL\____                           /: ]
         *            .__/°         °\___/°   \                         / ::\
         *           /^^ \            °  °     \_______.__________.____/: OO:\
         *      .__./     j      ________             _________________ ::OO::|
         *    ./ ^^ j____/°     [\______/]      .____/                 \__:__/
         *  ._|____/°    °       <{(OMG{<       /                         ::
         * /  °    °              (OMFG{       /
         * |°  loooooooooooooooooooooooooooooooool
         *         °L|                   L|
         *          ()                   ()
         *
         *
         *  http://open.spotify.com/track/2ZtQKBB8wDTtPPqDZhy7xZ
         *
         */
        
        SyslogServerEventIF e;
        if (isStructuredSyslog(msg)) {
            e = new StructuredSyslogServerEvent(msg, remoteAddress);
        } else {
            e = new SyslogServerEvent(msg, remoteAddress);

        }

        Message m = new Message(e.getMessage(), parseHost(e, remoteAddress), parseDate(e));
        m.addField("facility", Tools.syslogFacilityToReadable(e.getFacility()));
        m.addField("level", e.getLevel());

        // Store full message if configured.
        if (config.getBoolean(SyslogInputBase.CK_STORE_FULL_MESSAGE)) {
            m.addField("full_message", new String(e.getRaw()));
        }

        m.addFields(parseAdditionalData(e));
        
        tcx.stop();

        return m;
    }

    private Map<String, Object> parseAdditionalData(SyslogServerEventIF msg) {
        Map<String, Object> structuredData = Maps.newHashMap();
        
        // Structured syslog has more data we can parse.
        if (msg instanceof StructuredSyslogServerEvent) {
            StructuredSyslogServerEvent sMsg = (StructuredSyslogServerEvent) msg;
            
            structuredData = StructuredSyslog.extractFields(sMsg);
            
            if (sMsg.getApplicationName() != null && !sMsg.getApplicationName().isEmpty()) {
                structuredData.put("application_name", sMsg.getApplicationName());
            }
            
            if (sMsg.getProcessId() != null && !sMsg.getProcessId().isEmpty()) {
                structuredData.put("process_id", sMsg.getProcessId());
            }
        }

        return structuredData;
    }

    private String parseHost(SyslogServerEventIF msg, InetAddress remoteAddress) {
        if (remoteAddress != null && config.getBoolean(SyslogInputBase.CK_FORCE_RDNS)) {
            try {
                return Tools.rdnsLookup(remoteAddress);
            } catch (UnknownHostException e) {
                LOG.warn("Reverse DNS lookup failed. Falling back to parsed hostname.", e);
            }
        }

        return msg.getHost();
    }

    private DateTime parseDate(SyslogServerEventIF msg) throws IllegalStateException {
        // Check if date could be parsed.
        if (msg.getDate() == null) {
            if (config.getBoolean(SyslogInputBase.CK_ALLOW_OVERRIDE_DATE)) {
                LOG.info("Date could not be parsed. Was set to NOW because {} is true.", SyslogInputBase.CK_ALLOW_OVERRIDE_DATE);
                return new DateTime();
            } else {
                LOG.info("Syslog message is missing date or date could not be parsed. (Possibly set {} to true) "
                        + "Not further handling. Message was: {}", SyslogInputBase.CK_ALLOW_OVERRIDE_DATE, new String(msg.getRaw()));
                throw new IllegalStateException();
            }
        }

        return new DateTime(msg.getDate());
    }

    /**
     * Try to find out if the message is a structured syslog message or not.
     * See ROFLCopter comment in parse() for a explanation of this. Sorry.
     * 
     * @param message
     * @return 
     */
    public static boolean isStructuredSyslog(String message) {
        return STRUCTURED_SYSLOG_PATTERN.matcher(message).matches();
    }
    
}
