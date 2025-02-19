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
package org.graylog.plugins.otel.input.codec;

import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.plugins.otel.input.OTelJournal;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.utilities.ExceptionUtils;

import java.util.Optional;

public class OTelCodec implements Codec {
    public static final String NAME = "OpenTelemetry";

    private final Configuration configuration;
    private final OTelLogsCodec logsCodec;

    @Inject
    public OTelCodec(@Assisted Configuration configuration, OTelLogsCodec logsCodec) {
        this.configuration = configuration;
        this.logsCodec = logsCodec;
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<OTelCodec> {
        @Override
        OTelCodec create(Configuration configuration);

        @Override
        Codec.Config getConfig();

        @Override
        Codec.Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            // Keep only source override config. Charset override is not being used.
            final ConfigurationRequest cr = new ConfigurationRequest();
            cr.addField(super.getRequestedConfiguration().getField(CK_OVERRIDE_SOURCE));

            return cr;
        }
    }

    @Override
    public Optional<Message> decodeSafe(@Nonnull RawMessage rawMessage) {
        final OTelJournal.Record journalRecord;
        try {
            journalRecord = OTelJournal.Record.parseFrom(rawMessage.getPayload());
        } catch (InvalidProtocolBufferException e) {
            throw InputProcessingException.create(
                    "Error parsing OpenTelemetry message", ExceptionUtils.getRootCause(e), rawMessage);
        }

        return switch (journalRecord.getPayloadCase()) {
            case LOG ->
                    logsCodec.decode(journalRecord.getLog(), rawMessage.getTimestamp(), rawMessage.getRemoteAddress());
            case PAYLOAD_NOT_SET -> throw InputProcessingException.create(
                    "Error handling OpenTelemetry message. No payload set.", rawMessage);
        };
    }

    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public @Nonnull Configuration getConfiguration() {
        return configuration;
    }
}
