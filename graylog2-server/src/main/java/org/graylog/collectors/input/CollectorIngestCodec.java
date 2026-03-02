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
import org.graylog.collectors.input.debug.OtlpTrafficDump;
import org.graylog.collectors.input.processor.LogRecordProcessor;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.inputs.otel.codec.OTelTypeConverter;
import org.graylog.schema.EventFields;
import org.graylog.schema.VendorFields;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * A codec for Collector Ingest inputs that produces minimal field mapping (no {@code otel_*} prefixed fields).
 * Extracts body as message, timestamp, and severityText as level from the journal record.
 */
public class CollectorIngestCodec implements Codec {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorIngestCodec.class);
    public static final String NAME = "CollectorIngest";

    private final Configuration configuration;
    private final MessageFactory messageFactory;
    private final OtlpTrafficDump dumpWriter;
    private final OTelTypeConverter typeConverter;
    private final Map<String, LogRecordProcessor> processors;

    @Inject
    public CollectorIngestCodec(@Assisted Configuration configuration,
                                MessageFactory messageFactory,
                                OtlpTrafficDump dumpWriter,
                                OTelTypeConverter typeConverter,
                                Map<String, LogRecordProcessor> processors) {
        this.configuration = configuration;
        this.messageFactory = messageFactory;
        this.dumpWriter = dumpWriter;
        this.typeConverter = typeConverter;
        this.processors = processors;
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

        dumpWriter.write(collectorRecord);

        final OTelJournal.Record otelRecord = collectorRecord.getOtelRecord();
        return switch (otelRecord.getPayloadCase()) {
            case LOG -> decodeLog(collectorRecord, rawMessage);
            case PAYLOAD_NOT_SET -> throw InputProcessingException.create(
                    "Error handling Collector Ingest message. No payload set.", rawMessage);
        };
    }

    private Optional<Message> decodeLog(CollectorJournal.Record collectorRecord, RawMessage rawMessage) {
        final var logRecord = collectorRecord.getOtelRecord().getLog().getLogRecord();
        final var receiverType = collectorRecord.getCollectorReceiverType();
        final var instanceUid = collectorRecord.getCollectorInstanceUid();

        if (isBlank(receiverType)) {
            LOG.warn("No collector receiver type found for log record {}", logRecord);
            return Optional.empty();
        }

        final var processor = processors.get(receiverType);
        if (processor == null) {
            LOG.warn("No collector processor found for receiver type: {}", receiverType);
            return Optional.empty();
        }

        final String body = typeConverter.toString(logRecord.getBody(), "body").orElse("");
        final String source = source(rawMessage.getRemoteAddress());
        final DateTime timestamp = timestamp(logRecord).orElse(rawMessage.getTimestamp());

        final Message message = messageFactory.createMessage(body, source, timestamp);

        message.addField("gl2_collector_receiver_type", receiverType);

        if (!instanceUid.isEmpty()) {
            message.addField(Message.FIELD_GL2_SOURCE_COLLECTOR, instanceUid);
        }

        if (!logRecord.getSeverityText().isEmpty()) {
            message.addField(VendorFields.VENDOR_EVENT_SEVERITY, logRecord.getSeverityText());
        }
        if (logRecord.getSeverityNumberValue() > 0) {
            message.addField(VendorFields.VENDOR_EVENT_SEVERITY_LEVEL, logRecord.getSeverityNumberValue());
        }
        if (logRecord.getTimeUnixNano() > 0) {
            message.addField(EventFields.EVENT_CREATED, Tools.buildElasticSearchTimeFormat(dateTimeFromNano(logRecord.getTimeUnixNano())));
        }
        if (logRecord.getObservedTimeUnixNano() > 0) {
            message.addField(EventFields.EVENT_RECEIVED_TIME, Tools.buildElasticSearchTimeFormat(dateTimeFromNano(logRecord.getObservedTimeUnixNano())));
        }

        // TODO: Surface processor errors (e.g. malformed body JSON) as message.addProcessingError()
        //  instead of silently returning an empty map. Requires changing the LogRecordProcessor interface.
        message.addFields(processor.process(logRecord));

        return Optional.of(message);
    }

    private DateTime dateTimeFromNano(long ts) {
        return new DateTime(ts / 1_000_000L, DateTimeZone.UTC);
    }

    private Optional<DateTime> timestamp(io.opentelemetry.proto.logs.v1.LogRecord logRecord) {
        if (logRecord.getTimeUnixNano() > 0) {
            return Optional.of(dateTimeFromNano(logRecord.getTimeUnixNano()));
        }
        if (logRecord.getObservedTimeUnixNano() > 0) {
            return Optional.of(dateTimeFromNano(logRecord.getObservedTimeUnixNano()));
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
