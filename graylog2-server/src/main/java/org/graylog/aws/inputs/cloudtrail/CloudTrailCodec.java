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
package org.graylog.aws.inputs.cloudtrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.aws.AWS;
import org.graylog.aws.AWSObjectMapper;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecord;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.util.Optional;

public class CloudTrailCodec extends AbstractCodec {
    public static final String NAME = "AWSCloudTrail";

    private final ObjectMapper objectMapper;
    private final MessageFactory messageFactory;

    @Inject
    public CloudTrailCodec(@Assisted Configuration configuration, @AWSObjectMapper ObjectMapper objectMapper,
                           MessageFactory messageFactory) {
        super(configuration);
        this.objectMapper = objectMapper;
        this.messageFactory = messageFactory;
    }

    @Override
    public Optional<Message> decodeSafe(@Nonnull RawMessage rawMessage) {
        try {
            final CloudTrailRecord record = objectMapper.readValue(rawMessage.getPayload(), CloudTrailRecord.class);
            final String source = configuration.getString(Config.CK_OVERRIDE_SOURCE, "aws-cloudtrail");
            final Message message = messageFactory.createMessage(record.getConstructedMessage(), source, DateTime.parse(record.eventTime));

            message.addFields(record.additionalFieldsAsMap());
            message.addField("full_message", record.getFullMessage());
            message.addField(AWS.SOURCE_GROUP_IDENTIFIER, true);

            return Optional.of(message);
        } catch (Exception e) {
            throw InputProcessingException.create("Could not deserialize CloudTrail record.",
                    e, rawMessage, new String(rawMessage.getPayload(), charset));
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<CloudTrailCodec> {
        @Override
        CloudTrailCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
    }
}
