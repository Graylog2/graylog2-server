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
package org.graylog2.inputs.codecs;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.inputs.codecs.gelf.GELFMessage;
import org.graylog2.inputs.transports.TcpTransport;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@Codec(name = "gelf", displayName = "GELF")
public class GelfCodec extends AbstractCodec {
    private static final Logger log = LoggerFactory.getLogger(GelfCodec.class);
    private static final String CK_DECOMPRESS_SIZE_LIMIT = "decompress_size_limit";
    private static final int DEFAULT_DECOMPRESS_SIZE_LIMIT = 8388608;

    private final GelfChunkAggregator aggregator;
    private final ObjectMapper objectMapper;
    private final long decompressSizeLimit;

    @Inject
    public GelfCodec(@Assisted Configuration configuration, GelfChunkAggregator aggregator) {
        super(configuration);
        this.aggregator = aggregator;
        this.objectMapper = new ObjectMapper().enable(
            JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS,
            JsonParser.Feature.ALLOW_TRAILING_COMMA);
        this.decompressSizeLimit = configuration.getInt(CK_DECOMPRESS_SIZE_LIMIT, DEFAULT_DECOMPRESS_SIZE_LIMIT);
    }

    private static String stringValue(final JsonNode json, final String fieldName) {
        if (json != null) {
            final JsonNode value = json.get(fieldName);

            if (value != null) {
                return value.asText();
            }
        }
        return null;
    }

    private static long longValue(final JsonNode json, final String fieldName) {
        if (json != null) {
            final JsonNode value = json.get(fieldName);

            if (value != null) {
                return value.asLong(-1L);
            }
        }
        return -1L;
    }

    private static int intValue(final JsonNode json, final String fieldName) {
        if (json != null) {
            final JsonNode value = json.get(fieldName);

            if (value != null) {
                return value.asInt(-1);
            }
        }
        return -1;
    }

    private static double timestampValue(final JsonNode json) {
        final JsonNode value = json.path(Message.FIELD_TIMESTAMP);
        if (value.isNumber()) {
            return value.asDouble(-1.0);
        } else if (value.isTextual()) {
            try {
                return Double.parseDouble(value.asText());
            } catch (NumberFormatException e) {
                log.debug("Unable to parse timestamp", e);
                return -1.0;
            }
        } else {
            return -1.0;
        }
    }

    @Nullable
    @Override
    public Message decode(@Nonnull final RawMessage rawMessage) {
        final GELFMessage gelfMessage = new GELFMessage(rawMessage.getPayload(), rawMessage.getRemoteAddress());
        final String json = gelfMessage.getJSON(decompressSizeLimit);

        final JsonNode node;

        try {
            node = objectMapper.readTree(json);
            if (node == null) {
                throw new IOException("null result");
            }
        } catch (final Exception e) {
            log.error("Could not parse JSON, first 400 characters: " +
                              StringUtils.abbreviate(json, 403), e);
            throw new IllegalStateException("JSON is null/could not be parsed (invalid JSON)", e);
        }

        validateGELFMessage(node, rawMessage.getId(), rawMessage.getRemoteAddress());

        // Timestamp.
        final double messageTimestamp = timestampValue(node);
        final DateTime timestamp;
        if (messageTimestamp <= 0) {
            timestamp = rawMessage.getTimestamp();
        } else {
            // we treat this as a unix timestamp
            timestamp = Tools.dateTimeFromDouble(messageTimestamp);
        }

        final Message message = new Message(
                stringValue(node, "short_message"),
                stringValue(node, "host"),
                timestamp
        );

        message.addField(Message.FIELD_FULL_MESSAGE, stringValue(node, "full_message"));

        final String file = stringValue(node, "file");

        if (file != null && !file.isEmpty()) {
            message.addField("file", file);
        }

        final long line = longValue(node, "line");
        if (line > -1) {
            message.addField("line", line);
        }

        // Level is set by server if not specified by client.
        final int level = intValue(node, "level");
        if (level > -1) {
            message.addField("level", level);
        }

        // Facility is set by server if not specified by client.
        final String facility = stringValue(node, "facility");
        if (facility != null && !facility.isEmpty()) {
            message.addField("facility", facility);
        }

        // Add additional data if there is some.
        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();

            String key = entry.getKey();
            // Do not index useless GELF "version" field.
            if ("version".equals(key)) {
                continue;
            }

            // Don't include GELF syntax underscore in message field key.
            if (key.startsWith("_") && key.length() > 1) {
                key = key.substring(1);
            }

            // We already set short_message and host as message and source. Do not add as fields again.
            if ("short_message".equals(key) || "host".equals(key)) {
                continue;
            }

            // Skip standard or already set fields.
            if (message.getField(key) != null || Message.RESERVED_FIELDS.contains(key) && !Message.RESERVED_SETTABLE_FIELDS.contains(key)) {
                continue;
            }

            // Convert JSON containers to Strings, and pick a suitable number representation.
            final JsonNode value = entry.getValue();

            final Object fieldValue;
            if (value.isContainerNode()) {
                fieldValue = value.toString();
            } else if (value.isFloatingPointNumber()) {
                fieldValue = value.asDouble();
            } else if (value.isIntegralNumber()) {
                fieldValue = value.asLong();
            } else if (value.isNull()) {
                log.debug("Field [{}] is NULL. Skipping.", key);
                continue;
            } else if (value.isTextual()) {
                fieldValue = value.asText();
            } else {
                log.debug("Field [{}] has unknown value type. Skipping.", key);
                continue;
            }

            message.addField(key, fieldValue);
        }

        return message;
    }

    private void validateGELFMessage(JsonNode jsonNode, UUID id, ResolvableInetSocketAddress remoteAddress) {
        final String prefix = "GELF message <" + id + "> " + (remoteAddress == null ? "" : "(received from <" + remoteAddress + ">) ");

        final JsonNode hostNode = jsonNode.path("host");
        if (hostNode.isMissingNode()) {
            log.warn(prefix + "is missing mandatory \"host\" field.");
        } else {
            if (!hostNode.isTextual()) {
                throw new IllegalArgumentException(prefix + "has invalid \"host\": " + hostNode.asText());
            }
            if (StringUtils.isBlank(hostNode.asText())) {
                throw new IllegalArgumentException(prefix + "has empty mandatory \"host\" field.");
            }
        }

        final JsonNode shortMessageNode = jsonNode.path("short_message");
        final JsonNode messageNode = jsonNode.path("message");
        if (!shortMessageNode.isMissingNode()) {
            if (!shortMessageNode.isTextual()) {
                throw new IllegalArgumentException(prefix + "has invalid \"short_message\": " + shortMessageNode.asText());
            }
            if (StringUtils.isBlank(shortMessageNode.asText()) && StringUtils.isBlank(messageNode.asText())) {
                throw new IllegalArgumentException(prefix + "has empty mandatory \"short_message\" field.");
            }
        } else if (!messageNode.isMissingNode()) {
            if (!messageNode.isTextual()) {
                throw new IllegalArgumentException(prefix + "has invalid \"message\": " + messageNode.asText());
            }
            if (StringUtils.isBlank(messageNode.asText())) {
                throw new IllegalArgumentException(prefix + "has empty mandatory \"message\" field.");
            }
        } else {
            throw new IllegalArgumentException(prefix + "is missing mandatory \"short_message\" or \"message\" field.");
        }

        final JsonNode timestampNode = jsonNode.path("timestamp");
        if (timestampNode.isValueNode() && !timestampNode.isNumber()) {
            log.warn(prefix + "has invalid \"timestamp\": {}  (type: {})", timestampNode.asText(), timestampNode.getNodeType().name());
        }
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return aggregator;
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<GelfCodec> {
        @Override
        GelfCodec create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest requestedConfiguration = super.getRequestedConfiguration();
            requestedConfiguration.addField(new NumberField(
                CK_DECOMPRESS_SIZE_LIMIT,
                "Decompressed size limit",
                DEFAULT_DECOMPRESS_SIZE_LIMIT,
                "The maximum number of bytes after decompression.",
                ConfigurationField.Optional.OPTIONAL));

            return requestedConfiguration;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(12201);
            }

            // GELF TCP always needs null-byte delimiter!
            if (cr.containsField(TcpTransport.CK_USE_NULL_DELIMITER)) {
                cr.getField(TcpTransport.CK_USE_NULL_DELIMITER).setDefaultValue(true);
            }
        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(GelfCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
