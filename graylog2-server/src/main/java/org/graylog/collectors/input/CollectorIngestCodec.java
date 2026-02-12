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
package org.graylog.collectors.input;

import com.google.common.net.InetAddresses;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.collectors.CollectorJournal;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.ResolvableInetSocketAddress;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Optional;

/**
 * A codec for Collector Ingest inputs that produces minimal field mapping (no {@code otel_*} prefixed fields).
 * Extracts body as message, timestamp, and severityText as level from the journal record.
 */
public class CollectorIngestCodec implements Codec {
    public static final String NAME = "CollectorIngest";

    private final Configuration configuration;
    private final MessageFactory messageFactory;

    @Inject
    public CollectorIngestCodec(@Assisted Configuration configuration, MessageFactory messageFactory) {
        this.configuration = configuration;
        this.messageFactory = messageFactory;
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<CollectorIngestCodec> {
        @Override
        CollectorIngestCodec create(Configuration configuration);

        @Override
        Codec.Config getConfig();

        @Override
        Codec.Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
        }
    }

    @Override
    public Optional<Message> decodeSafe(@Nonnull RawMessage rawMessage) {
        final CollectorJournal.Record collectorRecord;
        try {
            collectorRecord = CollectorJournal.Record.parseFrom(rawMessage.getPayload());
        } catch (InvalidProtocolBufferException e) {
            throw InputProcessingException.create(
                    "Error parsing Collector Ingest message", ExceptionUtils.getRootCause(e), rawMessage);
        }

        final OTelJournal.Record otelRecord = collectorRecord.getOtelRecord();
        return switch (otelRecord.getPayloadCase()) {
            case LOG -> decodeLog(otelRecord, rawMessage);
            case PAYLOAD_NOT_SET -> throw InputProcessingException.create(
                    "Error handling Collector Ingest message. No payload set.", rawMessage);
        };
    }

    private Optional<Message> decodeLog(OTelJournal.Record record, RawMessage rawMessage) {
        final var log = record.getLog();
        final var logRecord = log.getLogRecord();

        final String body = logRecord.getBody().getStringValue();
        final String source = source(rawMessage.getRemoteAddress());
        final DateTime timestamp = timestamp(logRecord).orElse(rawMessage.getTimestamp());

        final Message message = messageFactory.createMessage(body, source, timestamp);

        if (!logRecord.getSeverityText().isEmpty()) {
            message.addField("level", logRecord.getSeverityText());
        }

        return Optional.of(message);
    }

    private Optional<DateTime> timestamp(io.opentelemetry.proto.logs.v1.LogRecord logRecord) {
        if (logRecord.getTimeUnixNano() > 0) {
            return Optional.of(new DateTime(logRecord.getTimeUnixNano() / 1_000_000L, DateTimeZone.UTC));
        }
        if (logRecord.getObservedTimeUnixNano() > 0) {
            return Optional.of(new DateTime(logRecord.getObservedTimeUnixNano() / 1_000_000L, DateTimeZone.UTC));
        }
        return Optional.empty();
    }

    private String source(ResolvableInetSocketAddress remoteAddress) {
        if (remoteAddress == null) {
            return "unknown";
        }
        if (remoteAddress.getHostName() != null) {
            return remoteAddress.getHostName();
        }
        return InetAddresses.toAddrString(remoteAddress.getAddress());
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
