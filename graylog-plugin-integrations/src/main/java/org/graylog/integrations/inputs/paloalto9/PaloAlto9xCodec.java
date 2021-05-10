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
package org.graylog.integrations.inputs.paloalto9;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.integrations.inputs.paloalto.PaloAltoMessageBase;
import org.graylog.integrations.inputs.paloalto.PaloAltoParser;
import org.graylog.schema.EventFields;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

import static org.graylog.integrations.inputs.paloalto.PaloAltoMessageType.CONFIG;
import static org.graylog.integrations.inputs.paloalto.PaloAltoMessageType.CORRELATION;
import static org.graylog.integrations.inputs.paloalto.PaloAltoMessageType.GLOBAL_PROTECT_9_1_3;
import static org.graylog.integrations.inputs.paloalto.PaloAltoMessageType.GLOBAL_PROTECT_PRE_9_1_3;
import static org.graylog.integrations.inputs.paloalto.PaloAltoMessageType.HIP;
import static org.graylog.integrations.inputs.paloalto.PaloAltoMessageType.SYSTEM;
import static org.graylog.integrations.inputs.paloalto.PaloAltoMessageType.THREAT;
import static org.graylog.integrations.inputs.paloalto.PaloAltoMessageType.TRAFFIC;
import static org.graylog.integrations.inputs.paloalto.PaloAltoMessageType.USERID;

public class PaloAlto9xCodec implements Codec {
    private static final Logger LOG = LoggerFactory.getLogger(PaloAlto9xCodec.class);

    static final String CK_STORE_FULL_MESSAGE = "store_full_message";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ssZZ");

    public static final String NAME = "PaloAlto9x";

    private final Configuration configuration;
    private final PaloAltoParser rawMessageParser;
    private final PaloAlto9xParser fieldProducer;

    @AssistedInject
    public PaloAlto9xCodec(@Assisted Configuration configuration, PaloAltoParser rawMessageParser, PaloAlto9xParser fieldProducer) {
        this.configuration = configuration;
        this.rawMessageParser = rawMessageParser;
        this.fieldProducer = fieldProducer;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        String s = new String(rawMessage.getPayload());
        LOG.trace("Received raw message: {}", s);

        PaloAltoMessageBase p = rawMessageParser.parse(s);

        // Return when error occurs parsing syslog header.
        if (p == null) {
            return null;
        }

        Message message = new Message(p.payload(), p.source(), p.timestamp());

        switch (p.panType()) {
            case "THREAT":
                message.addFields(fieldProducer.parseFields(THREAT, p.fields()));
                break;
            case "SYSTEM":
                message.addFields(fieldProducer.parseFields(SYSTEM, p.fields()));
                break;
            case "TRAFFIC":
                message.addFields(fieldProducer.parseFields(TRAFFIC, p.fields()));
                break;
            case "CONFIG":
                message.addFields(fieldProducer.parseFields(CONFIG, p.fields()));
                break;
            case "HIP-MATCH":
            case "HIPMATCH":
                message.addFields(fieldProducer.parseFields(HIP, p.fields()));
                break;
            case "CORRELATION":
                message.addFields(fieldProducer.parseFields(CORRELATION, p.fields()));
                break;
            case "GLOBALPROTECT":
                // For PAN v9.1.3 and later, Global Protect has type in the expected position
                message.addFields(fieldProducer.parseFields(GLOBAL_PROTECT_9_1_3, p.fields()));
                break;
            case "USERID":
                message.addFields(fieldProducer.parseFields(USERID, p.fields()));
                break;
            default:
                //For PAN v9.1.2 and earlier, Global Protect has type in position 5 rather than position 3
                if (p.fields().get(5).equals("GLOBALPROTECT")) {
                    message.addFields(fieldProducer.parseFields(GLOBAL_PROTECT_PRE_9_1_3, p.fields()));
                    break;
                } else {
                    LOG.info("Received log for unsupported PAN type [{}]. Will not parse.", p.panType());
                }
        }

        message.addField(EventFields.EVENT_SOURCE_PRODUCT, "PAN");
        fixTimestampField(message);

        // Store full message if configured.
        if (configuration.getBoolean(CK_STORE_FULL_MESSAGE)) {
            message.addField(Message.FIELD_FULL_MESSAGE, new String(rawMessage.getPayload(), StandardCharsets.UTF_8));
        }

        LOG.trace("Successfully processed [{}] message with [{}] fields.", p.panType(), message.getFieldCount());

        return message;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Nonnull
    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<PaloAlto9xCodec> {
        @Override
        PaloAlto9xCodec create(Configuration configuration);

        @Override
        PaloAlto9xCodec.Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = new ConfigurationRequest();

            r.addField(
                    new BooleanField(
                            CK_STORE_FULL_MESSAGE,
                            "Store full message?",
                            false,
                            "Store the full original Palo Alto message as full_message?"
                    )
            );

            return r;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        }
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    private void fixTimestampField(Message message) {
        Object timestampObj = message.getField(Message.FIELD_TIMESTAMP);
        if (timestampObj instanceof String) {
            String timestampString = timestampObj + "Z";
            DateTime parsedTimestamp = DateTime.parse(timestampString, DATE_TIME_FORMATTER).withZone(DateTimeZone.UTC);
            message.addField(Message.FIELD_TIMESTAMP, parsedTimestamp);
        }
    }
}
