/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.integrations.aws.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.integrations.aws.cloudwatch.FlowLogMessage;
import org.graylog.integrations.aws.cloudwatch.IANAProtocolNumbers;
import org.graylog.integrations.aws.cloudwatch.KinesisLogEntry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.joda.time.Seconds;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KinesisCloudWatchFlowLogCodec extends AbstractKinesisCodec {
    public static final String NAME = "FlowLog";
    static final String FIELD_ACCOUNT_ID = "account_id";
    static final String FIELD_INTERFACE_ID = "interface_id";
    static final String FIELD_SRC_ADDR = "src_addr";
    static final String FIELD_DST_ADDR = "dst_addr";
    static final String FIELD_SRC_PORT = "src_port";
    static final String FIELD_DST_PORT = "dst_port";
    static final String FIELD_PROTOCOL_NUMBER = "protocol_number";
    static final String FIELD_PROTOCOL = "protocol";
    static final String FIELD_PACKETS = "packets";
    static final String FIELD_BYTES = "bytes";
    static final String FIELD_CAPTURE_WINDOW_DURATION = "capture_window_duration_seconds";
    static final String FIELD_ACTION = "action";
    static final String FIELD_LOG_STATUS = "log_status";
    static final String SOURCE = "aws-kinesis-flowlogs";
    private static final String FLOW_LOG_PREFIX = "flow_log_";

    private final IANAProtocolNumbers protocolNumbers;
    private final boolean noFlowLogPrefix;
    private final MessageFactory messageFactory;

    @Inject
    public KinesisCloudWatchFlowLogCodec(@Assisted Configuration configuration, ObjectMapper objectMapper, MessageFactory messageFactory) {
        super(configuration, objectMapper);
        this.messageFactory = messageFactory;
        this.protocolNumbers = new IANAProtocolNumbers();
        this.noFlowLogPrefix = configuration.getBoolean(AWSCodec.CK_FLOW_LOG_PREFIX, AWSCodec.FLOW_LOG_PREFIX_DEFAULT);
    }

    @Override
    public Optional<Message> decodeLogData(@Nonnull final KinesisLogEntry logEvent) {
        try {
            final FlowLogMessage flowLogMessage = FlowLogMessage.fromLogEvent(logEvent);

            final String source = configuration.getString(KinesisCloudWatchFlowLogCodec.Config.CK_OVERRIDE_SOURCE, SOURCE);
            final Message result = messageFactory.createMessage(
                    buildSummary(flowLogMessage),
                    source,
                    flowLogMessage.getTimestamp());
            result.addFields(buildFields(flowLogMessage));
            result.addField(FIELD_KINESIS_STREAM, logEvent.kinesisStream());
            result.addField(FIELD_LOG_GROUP, logEvent.logGroup());
            result.addField(FIELD_LOG_STREAM, logEvent.logStream());
            result.addField(SOURCE_GROUP_IDENTIFIER, true);

            return Optional.of(result);
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize AWS FlowLog record.", e);
        }
    }

    private String buildSummary(FlowLogMessage msg) {
        return new StringBuilder()
                .append(msg.getInterfaceId()).append(" ")
                .append(msg.getAction()).append(" ")
                .append(protocolNumbers.lookup(msg.getProtocolNumber())).append(" ")
                .append(msg.getSourceAddress()).append(":").append(msg.getSourcePort())
                .append(" -> ")
                .append(msg.getDestinationAddress()).append(":").append(msg.getDestinationPort())
                .toString();
    }

    private Map<String, Object> buildFields(FlowLogMessage msg) {

        final String prefix = this.noFlowLogPrefix ? "" : FLOW_LOG_PREFIX;
        final HashMap<String, Object> fields = new HashMap<>();
        fields.put(prefix + FIELD_ACCOUNT_ID, msg.getAccountId());
        fields.put(prefix + FIELD_INTERFACE_ID, msg.getInterfaceId());
        fields.put(prefix + FIELD_SRC_ADDR, msg.getSourceAddress());
        fields.put(prefix + FIELD_DST_ADDR, msg.getDestinationAddress());
        fields.put(prefix + FIELD_SRC_PORT, msg.getSourcePort());
        fields.put(prefix + FIELD_DST_PORT, msg.getDestinationPort());
        fields.put(prefix + FIELD_PROTOCOL_NUMBER, msg.getProtocolNumber());
        fields.put(prefix + FIELD_PROTOCOL, protocolNumbers.lookup(msg.getProtocolNumber()));
        fields.put(prefix + FIELD_PACKETS, msg.getPackets());
        fields.put(prefix + FIELD_BYTES, msg.getBytes());
        fields.put(prefix + FIELD_CAPTURE_WINDOW_DURATION, Seconds.secondsBetween(msg.getCaptureWindowStart(), msg.getCaptureWindowEnd()).getSeconds());
        fields.put(prefix + FIELD_ACTION, msg.getAction());
        fields.put(prefix + FIELD_LOG_STATUS, msg.getLogStatus());
        return fields;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<KinesisCloudWatchFlowLogCodec> {
        @Override
        KinesisCloudWatchFlowLogCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest config = new ConfigurationRequest();

            return config;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        }
    }
}
