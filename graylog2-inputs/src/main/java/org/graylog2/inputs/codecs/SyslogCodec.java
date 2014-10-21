/**
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
 */
/**
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
 */
package org.graylog2.inputs.codecs;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.syslog4j.server.SyslogServerEventIF;
import org.graylog2.syslog4j.server.impl.event.SyslogServerEvent;
import org.graylog2.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Throwables.propagate;

public class SyslogCodec implements Codec {
    private static final Logger log = LoggerFactory.getLogger(SyslogCodec.class);

    private final Configuration configuration;

    private static final Pattern STRUCTURED_SYSLOG_PATTERN = Pattern.compile("<\\d+>\\d.*", Pattern.DOTALL);

    public static final String CK_FORCE_RDNS = "force_rdns";
    public static final String CK_ALLOW_OVERRIDE_DATE = "allow_override_date";
    public static final String CK_STORE_FULL_MESSAGE = "store_full_message";

    @AssistedInject
    public SyslogCodec(@Assisted Configuration configuration) {
        this.configuration = configuration;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        final String msg = new String(rawMessage.getPayload(), Charsets.UTF_8);
        try {
            final InetSocketAddress remoteAddress = rawMessage.getRemoteAddress();
            return parse(msg, remoteAddress.getAddress(), rawMessage.getTimestamp());
        } catch (ClassCastException e) {
            propagate(e);
        }
        return null;
    }

    private Message parse(String msg, InetAddress remoteAddress, DateTime receivedTimestamp) {
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

        final SyslogServerEventIF e;
        if (STRUCTURED_SYSLOG_PATTERN.matcher(msg).matches()) {
            e = new StructuredSyslogServerEvent(msg, remoteAddress);
        } else {
            e = new SyslogServerEvent(msg, remoteAddress);

        }

        final Message m = new Message(e.getMessage(), parseHost(e, remoteAddress), parseDate(e, receivedTimestamp));
        m.addField("facility", Tools.syslogFacilityToReadable(e.getFacility()));
        m.addField("level", e.getLevel());

        // Store full message if configured.
        if (configuration.getBoolean(CK_STORE_FULL_MESSAGE)) {
            m.addField("full_message", new String(e.getRaw(), StandardCharsets.UTF_8));
        }

        m.addFields(parseAdditionalData(e));

        return m;
    }

    private Map<String, Object> parseAdditionalData(SyslogServerEventIF msg) {
        Map<String, Object> structuredData = Maps.newHashMap();

        // Structured syslog has more data we can parse.
        if (msg instanceof StructuredSyslogServerEvent) {
            final StructuredSyslogServerEvent sMsg = (StructuredSyslogServerEvent) msg;

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
        if (remoteAddress != null && configuration.getBoolean(CK_FORCE_RDNS)) {
            try {
                return Tools.rdnsLookup(remoteAddress);
            } catch (UnknownHostException e) {
                log.warn("Reverse DNS lookup failed. Falling back to parsed hostname.", e);
            }
        }

        return msg.getHost();
    }

    private DateTime parseDate(SyslogServerEventIF msg, DateTime receivedTimestamp) throws IllegalStateException {
        // Check if date could be parsed.
        if (msg.getDate() == null) {
            if (configuration.getBoolean(CK_ALLOW_OVERRIDE_DATE)) {
                log.debug("Date could not be parsed. Was set to NOW because {} is true.",
                          CK_ALLOW_OVERRIDE_DATE);
                return receivedTimestamp;
            } else {
                log.warn("Syslog message is missing date or date could not be parsed. (Possibly set {} to true) "
                                 + "Not further handling. Message was: {}",
                         CK_ALLOW_OVERRIDE_DATE, new String(msg.getRaw(), StandardCharsets.UTF_8));
                throw new IllegalStateException();
            }
        }

        return new DateTime(msg.getDate());
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @Override
    public String getName() {
        return "syslog";
    }

    @Nonnull
    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest r = new ConfigurationRequest();

        r.addField(
                new BooleanField(
                        CK_FORCE_RDNS,
                        "Force rDNS?",
                        false,
                        "Force rDNS resolution of hostname? Use if hostname cannot be parsed."
                )
        );

        r.addField(
                new BooleanField(
                        CK_ALLOW_OVERRIDE_DATE,
                        "Allow overriding date?",
                        true,
                        "Allow to override with current date if date could not be parsed?"
                )
        );

        r.addField(
                new BooleanField(
                        CK_STORE_FULL_MESSAGE,
                        "Store full message?",
                        false,
                        "Store the full original syslog message as full_message?"
                )
        );

        return r;
    }

    @Override
    public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        if (cr.containsField(NettyTransport.CK_PORT)) {
            cr.getField(NettyTransport.CK_PORT).setDefaultValue(514);
        }
    }

    public interface Factory extends Codec.Factory<SyslogCodec> {
        @Override
        SyslogCodec create(Configuration configuration);
    }

    /**
     * Parses structured syslog data.
     *
     * @author Lennart Koopmann <lennart@socketfeed.com>
     */
    public static class StructuredSyslog {

        private static final Logger LOG = LoggerFactory.getLogger(StructuredSyslog.class);

        public static Map<String, Object> extractFields(StructuredSyslogServerEvent msg) {
            Map<String, Object> fields = Maps.newHashMap();
            try {
                Map raw = msg.getStructuredMessage().getStructuredData();
                if (raw != null && raw.size() > 0) {
                    // Parsing this structured syslog message results in the following nested Map structure.
                    // "<165>1 2012-12-25T22:14:15.003Z mymachine evntslog - - [exampleSDID@32473 iut=\"3\" eventID=\"1011\"][meta sequenceId=\"1\"] message"
                    // {exampleSDID@32473={eventID=1011, iut=3}, meta={sequenceId=1}}
                    //
                    // TODO: If two different RFC5424 SD-ELEMENTS share the same SD-PARAM keys, they overwrites each other.
                    // Example: [test1 test="v1"][test2 test="v2"] might result in "test"="v2" in the fields map.
                    // Order is not guaranteed in the current syslog4j implementation!

                    for (Object o : raw.entrySet()) {
                        final Map.Entry entry = (Map.Entry) o;

                        if (entry.getValue() instanceof Map) {
                            fields.putAll((Map) entry.getValue());
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Could not extract structured syslog", e);
                return Maps.newHashMap();
            }

            return fields;
        }

    }
}
