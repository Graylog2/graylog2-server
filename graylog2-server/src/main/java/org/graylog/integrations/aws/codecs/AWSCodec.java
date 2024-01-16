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

import com.google.inject.assistedinject.Assisted;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Inject;

import java.util.Map;
import java.util.stream.Collectors;

public class AWSCodec extends AbstractCodec {

    public static final String NAME = "AWSCodec";
    private static final Logger LOG = LoggerFactory.getLogger(AWSCodec.class);

    /**
     * Specifies one of the {@code AWSInputType} choices, which indicates which codec and transport
     * should be used.
     */
    public static final String CK_AWS_MESSAGE_TYPE = "aws_message_type";
    public static final String CK_FLOW_LOG_PREFIX = "aws_flow_log_prefix";

    static final boolean FLOW_LOG_PREFIX_DEFAULT = true;

    private final Map<String, Codec.Factory<? extends Codec>> availableCodecs;

    @Inject
    public AWSCodec(@Assisted Configuration configuration,
                    Map<String, Codec.Factory<? extends Codec>> availableCodecs) {
        super(configuration);
        this.availableCodecs = availableCodecs;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {

        // Load the codec by message type.
        final AWSMessageType awsMessageType = AWSMessageType.valueOf(configuration.getString(CK_AWS_MESSAGE_TYPE));
        final Codec.Factory<? extends Codec> codecFactory = this.availableCodecs.get(awsMessageType.getCodecName());
        if (codecFactory == null) {
            LOG.error("A codec with name [{}] could not be found.", awsMessageType.getCodecName());
            return null;
        }

        final Codec codec = codecFactory.create(configuration);

        // Parse the message with the specified codec.
        final Message message = codec.decode(new RawMessage(rawMessage.getPayload()));
        if (message == null) {
            LOG.error("Failed to decode message for codec [{}].", codec.getName());
            return null;
        }

        return message;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<AWSCodec> {
        @Override
        AWSCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            ConfigurationRequest request = new ConfigurationRequest();

            request.addField(new DropdownField(
                    CK_AWS_MESSAGE_TYPE,
                    "AWS Message Type",
                    Region.US_EAST_1.id(),
                    AWSMessageType.getMessageTypes().stream()
                            .collect(Collectors.toMap(AWSMessageType::toString, AWSMessageType::getLabel)),
                    "The type of AWS message that this input will receive.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            request.addField(new BooleanField(
                    CK_FLOW_LOG_PREFIX,
                    "Add Flow Log field name prefix",
                    FLOW_LOG_PREFIX_DEFAULT,
                    "Add field with the Flow Log prefix e. g. \"src_addr\" -> \"flow_log_src_addr\"."
            ));

            return request;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        }
    }
}
