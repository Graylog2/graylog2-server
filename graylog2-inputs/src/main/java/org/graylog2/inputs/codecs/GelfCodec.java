/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.codecs;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.inputs.codecs.gelf.GELFMessage;
import org.graylog2.inputs.transports.TcpTransport;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

public class GelfCodec implements Codec {
    private static final Logger log = LoggerFactory.getLogger(GelfCodec.class);

    private final GelfChunkAggregator aggregator;
    private final ObjectMapper objectMapper;

    @Inject
    public GelfCodec(@Assisted Configuration configuration, GelfChunkAggregator aggregator) {
        this.aggregator = aggregator;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
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

    private static double doubleValue(final JsonNode json, final String fieldName) {
        if (json != null) {
            final JsonNode value = json.get(fieldName);

            if (value != null) {
                return value.asDouble(-1.0);
            }
        }
        return -1.0;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull final RawMessage rawMessage) {
        final GELFMessage gelfMessage = new GELFMessage(rawMessage.getPayload());
        final String json = gelfMessage.getJSON();

        final JsonNode node;

        try {
            node = objectMapper.readTree(json);
        } catch (final Exception e) {
            log.error("Could not parse JSON!", e);
            throw new IllegalStateException("JSON is null/could not be parsed (invalid JSON)", e);
        }

        // Timestamp.
        final double messageTimestamp = doubleValue(node, "timestamp");
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

        message.addField("full_message", stringValue(node, "full_message"));

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
        final String facility = stringValue(node, ("facility"));
        if (facility != null && !facility.isEmpty()) {
            message.addField("facility", facility);
        }

        // Add additional data if there is some.
        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();

            String key = entry.getKey();
            final JsonNode value = entry.getValue();

            // Don't include GELF syntax underscore in message field key.
            if (key.startsWith("_") && key.length() > 1) {
                key = key.substring(1);
            }

            // We already set short_message and host as message and source. Do not add as fields again.
            if (key.equals("short_message") || key.equals("host")) {
                continue;
            }

            // Skip standard or already set fields.
            if (message.getField(key) != null || Message.RESERVED_FIELDS.contains(key)) {
                continue;
            }

            // Convert JSON containers to Strings, and pick a suitable number representation.
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

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return aggregator;
    }

    @Override
    public String getName() {
        return "gelf";
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<GelfCodec> {
        @Override
        GelfCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
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
}
