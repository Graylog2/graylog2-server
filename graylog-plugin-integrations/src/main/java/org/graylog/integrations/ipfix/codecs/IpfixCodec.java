/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.integrations.ipfix.codecs;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.Unpooled;
import org.graylog.integrations.ipfix.Flow;
import org.graylog.integrations.ipfix.InformationElementDefinitions;
import org.graylog.integrations.ipfix.IpfixException;
import org.graylog.integrations.ipfix.IpfixJournal;
import org.graylog.integrations.ipfix.IpfixParser;
import org.graylog.integrations.ipfix.TemplateRecord;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.codecs.MultiMessageCodec;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Codec(name = "ipfix", displayName = "IPFIX Codec")
public class IpfixCodec extends AbstractCodec implements MultiMessageCodec {

    @VisibleForTesting
    static final String CK_IPFIX_DEFINITION_PATH = "ipfix_definition_path";

    private static final Logger LOG = LoggerFactory.getLogger(IpfixCodec.class);

    private final IpfixAggregator ipfixAggregator;
    private final IpfixParser parser;
    private InformationElementDefinitions infoElementDefs;

    @Inject
    protected IpfixCodec(@Assisted Configuration configuration, IpfixAggregator ipfixAggregator) throws IOException {
        super(configuration);
        this.ipfixAggregator = ipfixAggregator;
        final URL standardIPFixDefTemplate = Resources.getResource("ipfix-iana-elements.json");
        final String ipFixCustomDefPath    = (String) configuration.getSource().get(CK_IPFIX_DEFINITION_PATH);

        if (ipFixCustomDefPath == null || ipFixCustomDefPath.trim().isEmpty()) {
            infoElementDefs = new InformationElementDefinitions(standardIPFixDefTemplate);
        } else {
            final URL customIPFixDefURL = Paths.get(ipFixCustomDefPath).toUri().toURL();
            infoElementDefs = new InformationElementDefinitions(standardIPFixDefTemplate, customIPFixDefURL);
        }
        this.parser = new IpfixParser(this.infoElementDefs);
    }

    /**
     * Parses out the fields from the flow record, assigns v5 fixed format fields and create message
     *
     * @param record
     * @return
     */
    private static String toMessageString(Flow record) {
        LOG.debug("IPFIX message being assembled from flow record [{}].", record.fields());
        final ImmutableMap<String, Object> fields = record.fields();
        final long packetCount = (long) fields.getOrDefault("packetDeltaCount", 0L);
        long octetCount = (long) fields.getOrDefault("octetDeltaCount", 0L);
        if (octetCount == 0L) {
            octetCount = (long) fields.getOrDefault("fwd_flow_delta_bytes", 0L);
        }

        String srcAddr = (String) fields.get("sourceIPv4Address");
        String dstAddr = (String) fields.get("destinationIPv4Address");
        if (srcAddr == null) {
            srcAddr = (String) fields.get("sourceIPv6Address");
        }
        if (dstAddr == null) {
            dstAddr = (String) fields.get("destinationIPv6Address");
        }

        final Number srcPort = (Number) fields.get("sourceTransportPort");
        final Number dstPort = (Number) fields.get("destinationTransportPort");
        final long protocol = Long.parseLong(String.valueOf(fields.getOrDefault("protocolIdentifier", 0L)));

        // TODO should this be configurable?
        return createMessageString(packetCount, octetCount, srcAddr, dstAddr, srcPort, dstPort, protocol);
    }

    private static String createMessageString(long packetCount, long octetCount, String srcAddr, String dstAddr,
                                              Number srcPort, Number dstPort, long protocol) {
        String message = String.format(Locale.ROOT, "Ipfix [" + srcAddr + "]:" + srcPort + " <> [" + dstAddr + "]:" + dstPort + " " +
                                                    "proto:" + protocol + " pkts:" + packetCount + " bytes:" + octetCount);
        return message;
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return ipfixAggregator;
    }

    public InformationElementDefinitions getInfoElementDefs() {
        return infoElementDefs;
    }

    @Nullable
    @Override
    public Collection<Message> decodeMessages(@Nonnull RawMessage rawMessage) {
        LOG.debug("Attempting to decode raw messages now.");
        final ResolvableInetSocketAddress remoteAddress = rawMessage.getRemoteAddress();
        final InetSocketAddress sender = remoteAddress != null ? remoteAddress.getInetSocketAddress() : null;
        try {
            final IpfixJournal.RawIpfix rawIpfix = IpfixJournal.RawIpfix.parseFrom(rawMessage.getPayload());
            final Map<Integer, ByteString> templatesMap = rawIpfix.getTemplatesMap();

            final Map<Integer, TemplateRecord> templateRecordMap = Seq.seq(templatesMap)
                                                                      .map(entry -> entry.map2(byteString -> parser.parseTemplateRecord(Unpooled.wrappedBuffer(byteString.toByteArray()))))
                                                                      .toMap(Tuple2::v1, Tuple2::v2);

            return rawIpfix.getDataSetsList().stream()
                           .map(dataSet -> {
                               final int templateId = dataSet.getTemplateId();
                               final ZonedDateTime flowExportTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(dataSet.getTimestampEpochSeconds()), ZoneOffset.UTC);
                               final TemplateRecord templateRecord = templateRecordMap.get(templateId);
                               if (templateRecord == null) {
                                   throw new IpfixException("Missing required template in journal entry for data records: template id " + templateId);
                               }
                               final Set<Flow> flows = parser.parseDataSet(templateRecord.informationElements(),
                                                                           Unpooled.wrappedBuffer(dataSet.getDataRecords().toByteArray()));
                               return flows.stream()
                                           .map(flow -> formatFlow(flowExportTimestamp, sender, flow));
                           })
                           .flatMap(messageStream -> messageStream)
                           .collect(Collectors.toList());
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Unable to parse ipfix journal message", e);
            return Collections.emptyList();
        }
    }

    private Message formatFlow(ZonedDateTime flowExportTimestamp, InetSocketAddress sender, Flow flow) {
        final ImmutableMap<String, Object> fields = flow.fields();
        final DateTime timestamp = new DateTime(Date.from(flowExportTimestamp.toInstant()));
        final String source = sender == null ? null : sender.getAddress().getHostAddress();
        final Message message = new Message(toMessageString(flow), source, timestamp);
        message.addFields(fields);
        return message;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        throw new UnsupportedOperationException("MultiMessageCodec " + getClass() + " does not support decode()");
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<IpfixCodec> {
        @Override
        IpfixCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(4739);
            }
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configuration = super.getRequestedConfiguration();
            /*configuration.addField(
                    new TextField(CK_IPFIX_DEFINITION_PATH,
                            "IPFIX field definitions",
                            "",
                            "Path to the JSON file containing IPFIX field definitions",
                            ConfigurationField.Optional.OPTIONAL)
            );*/
            return configuration;
        }
    }
}
