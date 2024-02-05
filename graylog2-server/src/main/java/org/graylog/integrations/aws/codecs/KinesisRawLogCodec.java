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
import org.graylog.integrations.aws.cloudwatch.KinesisLogEntry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.Codec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Inject;

public class KinesisRawLogCodec extends AbstractKinesisCodec {
    public static final String NAME = "CloudWatchRawLog";
    static final String SOURCE = "aws-kinesis-raw-logs";

    @Inject
    public KinesisRawLogCodec(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        super(configuration, objectMapper);
    }

    @Nullable
    @Override
    public Message decodeLogData(@Nonnull final KinesisLogEntry logEvent) {
        try {
            final String source = configuration.getString(KinesisCloudWatchFlowLogCodec.Config.CK_OVERRIDE_SOURCE, SOURCE);
            Message result = new Message(
                    logEvent.message(),
                    source,
                    logEvent.timestamp()
            );
            result.addField(FIELD_KINESIS_STREAM, logEvent.kinesisStream());
            result.addField(FIELD_LOG_GROUP, logEvent.logGroup());
            result.addField(FIELD_LOG_STREAM, logEvent.logStream());

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize AWS FlowLog record.", e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<KinesisRawLogCodec> {
        @Override
        KinesisRawLogCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
    }
}
