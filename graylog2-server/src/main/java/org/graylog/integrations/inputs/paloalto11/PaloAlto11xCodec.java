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
package org.graylog.integrations.inputs.paloalto11;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.integrations.inputs.paloalto.PaloAltoMessageBase;
import org.graylog.integrations.inputs.paloalto.PaloAltoParser;
import org.graylog.schema.EventFields;
import org.graylog.schema.VendorFields;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

public class PaloAlto11xCodec implements Codec {
    private static final Logger LOG = LoggerFactory.getLogger(PaloAlto11xCodec.class);
    static final String CK_STORE_FULL_MESSAGE = "store_full_message";
    static final String CK_TIMEZONE = "timezone";

    public static final String NAME = "PaloAlto11x";
    private final Configuration configuration;
    private final MessageFactory messageFactory;
    private final PaloAltoParser rawMessageParser;
    private final DateTimeZone timezone;

    @AssistedInject
    public PaloAlto11xCodec(@Assisted Configuration configuration, PaloAltoParser rawMessageParser,
                            MessageFactory messageFactory) {
        this.configuration = configuration;
        this.messageFactory = messageFactory;
        String timezoneID = configuration.getString(CK_TIMEZONE);
        // previously existing PA inputs after updating will not have a Time Zone configured, default to UTC
        this.timezone = timezoneID != null ? DateTimeZone.forID(timezoneID) : DateTimeZone.UTC;
        LOG.trace("Configured with time zone: {}", timezone);
        this.rawMessageParser = rawMessageParser;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        String s = new String(rawMessage.getPayload(), StandardCharsets.UTF_8);
        LOG.trace("Received raw message: {}", s);
        PaloAltoMessageBase p = rawMessageParser.parse(s, timezone);
        // Return when error occurs parsing syslog header.
        if (p == null) {
            return null;
        }
        Message message = messageFactory.createMessage(p.payload(), p.source(), p.timestamp());
        message.addField(EventFields.EVENT_SOURCE_PRODUCT, "PAN");
        message.addField(VendorFields.VENDOR_SUBTYPE, p.panType());
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
    public interface Factory extends Codec.Factory<PaloAlto11xCodec> {
        @Override
        PaloAlto11xCodec create(Configuration configuration);

        @Override
        PaloAlto11xCodec.Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = new ConfigurationRequest();

            r.addField(new DropdownField(
                    CK_TIMEZONE,
                    "Time Zone",
                    DateTimeZone.UTC.getID(),
                    DropdownField.ValueTemplates.timeZones(),
                    "Time zone of the Palo Alto device",
                    ConfigurationField.Optional.OPTIONAL));

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
}
