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
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PaloAltoCodec implements Codec {

    public static final String NAME = "PaloAlto";

    public static final String CK_TRAFFIC_TEMPLATE = "TRAFFIC_TEMPLATE";
    public static final String CK_THREAT_TEMPLATE = "THREAT_TEMPLATE";
    public static final String CK_SYSTEM_TEMPLATE = "SYSTEM_TEMPLATE";

    private static final Logger LOG = LoggerFactory.getLogger(PaloAltoCodec.class);

    private final Configuration configuration;
    private final PaloAltoParser parser;
    private final PaloAltoTemplates templates;

    @AssistedInject
    public PaloAltoCodec(@Assisted Configuration configuration) {
        this.configuration = configuration;
        this.parser = new PaloAltoParser();
        this.templates = PaloAltoTemplates.newInstance(configuration.getString(CK_SYSTEM_TEMPLATE, PaloAltoTemplateDefaults.SYSTEM_TEMPLATE),
                                                       configuration.getString(CK_THREAT_TEMPLATE, PaloAltoTemplateDefaults.THREAT_TEMPLATE),
                                                       configuration.getString(CK_TRAFFIC_TEMPLATE, PaloAltoTemplateDefaults.TRAFFIC_TEMPLATE));
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        String s = new String(rawMessage.getPayload());
        LOG.trace("Received raw message: {}", s);

        PaloAltoMessageBase p = parser.parse(s);

        // Return when error occurs parsing syslog header.
        if (p == null) {
            return null;
        }

        Message message = new Message(p.payload(), p.source(), p.timestamp());

        switch (p.panType()) {
            case "THREAT":
                final PaloAltoTypeParser parserThreat = new PaloAltoTypeParser(templates.getThreatMessageTemplate());
                message.addFields(parserThreat.parseFields(p.fields()));
                break;
            case "SYSTEM":
                final PaloAltoTypeParser parserSystem = new PaloAltoTypeParser(templates.getSystemMessageTemplate());
                message.addFields(parserSystem.parseFields(p.fields()));
                break;
            case "TRAFFIC":
                final PaloAltoTypeParser parserTraffic = new PaloAltoTypeParser(templates.getTrafficMessageTemplate());
                message.addFields(parserTraffic.parseFields(p.fields()));
                break;
            default:
                LOG.error("Unsupported PAN type [{}]. Not adding any parsed fields.", p.panType());
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
        private static final String SYSTEM_MESSAGE_DESCRIPTION = "CSV string representing the fields/positions/data types to parse. (See documentation)";
        private static final String THREAT_MESSAGE_DESCRIPTION = "CSV string representing the fields/positions/data types to parse. (See documentation)";
        private static final String TRAFFIC_MESSAGE_DESCRIPTION = "CSV representing the fields/positions/data types to parse. (See documentation)";

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest request = new ConfigurationRequest();

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
