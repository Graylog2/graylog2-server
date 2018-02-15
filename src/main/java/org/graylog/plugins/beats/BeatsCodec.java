/**
 * This file is part of Graylog Beats Plugin.
 *
 * Graylog Beats Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Beats Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Beats Plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.beats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Codec(name = "beats", displayName = "Beats")
public class BeatsCodec extends AbstractCodec {
    private static final Logger LOG = LoggerFactory.getLogger(BeatsCodec.class);
    private static final String MAP_KEY_SEPARATOR = "_";
    private static final String BEATS_UNKNOWN = "unknown";
    private static final String CK_BEATS_PREFIX = "beats_prefix";

    private final ObjectMapper objectMapper;
    private final boolean useBeatPrefix;

    @Inject
    public BeatsCodec(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        super(configuration);

        this.useBeatPrefix = configuration.getBoolean(CK_BEATS_PREFIX, false);
        this.objectMapper = requireNonNull(objectMapper);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        final byte[] payload = rawMessage.getPayload();
        final JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (IOException e) {
            LOG.error("Couldn't decode raw message {}", rawMessage);
            return null;
        }

        return parseEvent(event);
    }

    private Message parseEvent(JsonNode event) {
        final String beatsType = event.path("@metadata").path("beat").asText("beat");
        final String rootPath = useBeatPrefix ? beatsType : "";
        final String message = event.path("message").asText("-");
        final String timestampField = event.path("@timestamp").asText();
        final DateTime timestamp = Tools.dateTimeFromString(timestampField);

        final JsonNode beat = event.path("beat");
        final String hostname = beat.path("hostname").asText(BEATS_UNKNOWN);

        final Message gelfMessage = new Message(message, hostname, timestamp);
        gelfMessage.addField("beats_type", beatsType);
        gelfMessage.addField("facility", "beats");

        addFlattened(gelfMessage, rootPath, event);
        return gelfMessage;
    }

    private void addFlattened(Message message, String currentPath, JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields();
            final String pathPrefix = currentPath.isEmpty() ? "" : currentPath + MAP_KEY_SEPARATOR;
            while (it.hasNext()) {
                final Map.Entry<String, JsonNode> entry = it.next();
                addFlattened(message, pathPrefix + entry.getKey(), entry.getValue());
            }
        } else if (jsonNode.isArray()) {
            final List<Object> values = new ArrayList<>(jsonNode.size());
            for (int i = 0; i < jsonNode.size(); i++) {
                final JsonNode currentNode = jsonNode.get(i);
                if (currentNode.isObject()) {
                    final String pathPrefix = currentPath.isEmpty() ? "" : currentPath + MAP_KEY_SEPARATOR + i;
                    addFlattened(message, pathPrefix, currentNode);
                } else if (currentNode.isValueNode()) {
                    values.add(valueNode(currentNode));
                }
            }
            message.addField(currentPath, values);
        } else if (jsonNode.isValueNode()) {
            message.addField(currentPath, valueNode(jsonNode));
        }
    }

    @Nullable
    private Object valueNode(JsonNode jsonNode) {
        if (jsonNode.isInt()) {
            return jsonNode.asInt();
        } else if (jsonNode.isLong()) {
            return jsonNode.asLong();
        } else if (jsonNode.isIntegralNumber()) {
            return jsonNode.asLong();
        } else if (jsonNode.isFloatingPointNumber()) {
            return jsonNode.asDouble();
        } else if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        } else if (jsonNode.isNull()) {
            return null;
        } else {
            return jsonNode.asText();
        }
    }


    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<BeatsCodec> {
        @Override
        BeatsCodec create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = super.getRequestedConfiguration();

            configurationRequest.addField(new BooleanField(
                    CK_BEATS_PREFIX,
                    "Add Beats type as prefix",
                    false,
                    "Use the Beats type as prefix for each field, e. g. \"filebeat_source\"."
            ));

            return configurationRequest;
        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(BeatsCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
