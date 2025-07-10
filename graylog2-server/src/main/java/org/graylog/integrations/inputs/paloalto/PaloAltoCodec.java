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
package org.graylog.integrations.inputs.paloalto;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PaloAltoCodec implements Codec {

    public static final String NAME = "PaloAlto";

    public static final String CK_TRAFFIC_TEMPLATE = "TRAFFIC_TEMPLATE";
    public static final String CK_THREAT_TEMPLATE = "THREAT_TEMPLATE";
    public static final String CK_SYSTEM_TEMPLATE = "SYSTEM_TEMPLATE";
    public static final String CK_TIMEZONE = "TIMEZONE";

    private static final Logger LOG = LoggerFactory.getLogger(PaloAltoCodec.class);

    private final Configuration configuration;
    private final MessageFactory messageFactory;
    private final PaloAltoParser parser;
    private final PaloAltoTemplates templates;

    @AssistedInject
    public PaloAltoCodec(@Assisted Configuration configuration, MessageFactory messageFactory) {
        this.configuration = configuration;
        this.messageFactory = messageFactory;
        this.parser = new PaloAltoParser();
        this.templates = PaloAltoTemplates.newInstance(configuration.getString(CK_SYSTEM_TEMPLATE, PaloAltoTemplateDefaults.SYSTEM_TEMPLATE),
                configuration.getString(CK_THREAT_TEMPLATE, PaloAltoTemplateDefaults.THREAT_TEMPLATE),
                configuration.getString(CK_TRAFFIC_TEMPLATE, PaloAltoTemplateDefaults.TRAFFIC_TEMPLATE));
    }

    @Override
    public Optional<Message> decodeSafe(@Nonnull RawMessage rawMessage) {
        String s = new String(rawMessage.getPayload(), StandardCharsets.UTF_8);
        LOG.trace("Received raw message: {}", s);

        String timezoneID = configuration.getString(CK_TIMEZONE);
        // previously existing PA inputs after updating will not have a Time Zone configured, default to UTC
        DateTimeZone timezone = timezoneID != null ? DateTimeZone.forID(timezoneID) : DateTimeZone.UTC;
        LOG.trace("Configured time zone: {}", timezone);
        try {
            PaloAltoMessageBase p = parser.parse(s, timezone);
            Message message = messageFactory.createMessage(p.payload(), p.source(), p.timestamp());

            switch (p.panType()) {
                case "THREAT":
                    final PaloAltoTypeParser parserThreat = new PaloAltoTypeParser(templates.getThreatMessageTemplate());
                    message.addFields(parserThreat.parseFields(p.fields(), timezone));
                    break;
                case "SYSTEM":
                    final PaloAltoTypeParser parserSystem = new PaloAltoTypeParser(templates.getSystemMessageTemplate());
                    message.addFields(parserSystem.parseFields(p.fields(), timezone));
                    break;
                case "TRAFFIC":
                    final PaloAltoTypeParser parserTraffic = new PaloAltoTypeParser(templates.getTrafficMessageTemplate());
                    message.addFields(parserTraffic.parseFields(p.fields(), timezone));
                    break;
                default:
                    LOG.error("Unsupported PAN type [{}]. Not adding any parsed fields.", p.panType());
            }

            LOG.trace("Successfully processed [{}] message with [{}] fields.", p.panType(), message.getFieldCount());

            return Optional.of(message);
        } catch (Exception e) {
            throw InputProcessingException.create("Could not decode PaloAlto9x message.", e, rawMessage, s);
        }
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
    public interface Factory extends Codec.Factory<PaloAltoCodec> {
        @Override
        PaloAltoCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {

        private static final String SYSTEM_MESSAGE_LABEL = "System Message Mappings";
        private static final String THREAT_MESSAGE_LABEL = "Threat Message Mappings";
        private static final String TRAFFIC_MESSAGE_LABEL = "Traffic Message Mappings";
        private static final String TIMEZONE_OFFSET_LABEL = "Time Zone";
        private static final String SYSTEM_MESSAGE_DESCRIPTION = "CSV string representing the fields/positions/data types to parse. (See documentation)";
        private static final String THREAT_MESSAGE_DESCRIPTION = "CSV string representing the fields/positions/data types to parse. (See documentation)";
        private static final String TRAFFIC_MESSAGE_DESCRIPTION = "CSV representing the fields/positions/data types to parse. (See documentation)";
        private static final String TIMEZONE_OFFSET_DESCRIPTION = "Time zone of the Palo Alto device";

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest request = new ConfigurationRequest();

            request.addField(new DropdownField(
                    CK_TIMEZONE,
                    TIMEZONE_OFFSET_LABEL,
                    DateTimeZone.UTC.getID(),
                    DropdownField.ValueTemplates.timeZones(),
                    TIMEZONE_OFFSET_DESCRIPTION,
                    ConfigurationField.Optional.OPTIONAL));

            request.addField(new TextField(
                    CK_SYSTEM_TEMPLATE,
                    SYSTEM_MESSAGE_LABEL,
                    PaloAltoTemplateDefaults.SYSTEM_TEMPLATE,
                    SYSTEM_MESSAGE_DESCRIPTION,
                    ConfigurationField.Optional.OPTIONAL, TextField.Attribute.TEXTAREA));

            request.addField(new TextField(
                    CK_THREAT_TEMPLATE,
                    THREAT_MESSAGE_LABEL,
                    PaloAltoTemplateDefaults.THREAT_TEMPLATE,
                    THREAT_MESSAGE_DESCRIPTION,
                    ConfigurationField.Optional.OPTIONAL, TextField.Attribute.TEXTAREA));

            request.addField(new TextField(
                    CK_TRAFFIC_TEMPLATE,
                    TRAFFIC_MESSAGE_LABEL,
                    PaloAltoTemplateDefaults.TRAFFIC_TEMPLATE,
                    TRAFFIC_MESSAGE_DESCRIPTION,
                    ConfigurationField.Optional.OPTIONAL, TextField.Attribute.TEXTAREA));

            return request;
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
