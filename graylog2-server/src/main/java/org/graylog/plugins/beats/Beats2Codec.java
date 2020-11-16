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
package org.graylog.plugins.beats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class Beats2Codec extends AbstractCodec {
    private static final Logger LOG = LoggerFactory.getLogger(Beats2Codec.class);
    private static final String MAP_KEY_SEPARATOR = "_";
    private static final String BEATS_UNKNOWN = "unknown";
    private static final String CK_NO_BEATS_PREFIX = "no_beats_prefix";

    private final ObjectMapper objectMapper;
    private final boolean noBeatsPrefix;

    @Inject
    public Beats2Codec(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        super(configuration);

        this.noBeatsPrefix = configuration.getBoolean(CK_NO_BEATS_PREFIX, false);
        this.objectMapper = requireNonNull(objectMapper);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        final byte[] payload = rawMessage.getPayload();
        final JsonNode event;
        try {
            event = objectMapper.readTree(payload);
            if (event == null) {
                throw new IOException("null result");
            }
        } catch (IOException e) {
            LOG.error("Couldn't decode raw message {}", rawMessage);
            return null;
        }

        return parseEvent(event);
    }

    private Message parseEvent(JsonNode event) {
        final String beatsType = event.path("@metadata").path("beat").asText("beat");
        final String rootPath = noBeatsPrefix ? "" : beatsType;
        final String message = event.path("message").asText("-");
        final String timestampField = event.path("@timestamp").asText();
        final DateTime timestamp = Tools.dateTimeFromString(timestampField);

        JsonNode agentOrBeat = event.path("agent");
        // backwards compatibility for beats < 7.0
        if (agentOrBeat.isMissingNode()) {
            agentOrBeat = event.path("beat");
        }
        final String hostname = agentOrBeat.path("hostname").asText(BEATS_UNKNOWN);

        final Message gelfMessage = new Message(message, hostname, timestamp);
        gelfMessage.addField("beats_type", beatsType);

        // This field should be stored without a prefix
        final String gl2SourceCollector = event.path(Message.FIELD_GL2_SOURCE_COLLECTOR).asText();
        if (!gl2SourceCollector.isEmpty()) {
            gelfMessage.addField(Message.FIELD_GL2_SOURCE_COLLECTOR, gl2SourceCollector);
        }

        // Remove fields that should not be duplicated with a prefix
        if (event.isObject()) {
            ObjectNode onode = (ObjectNode) event;
            onode.remove("message");
            onode.remove(Message.FIELD_GL2_SOURCE_COLLECTOR);
        }
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
    public interface Factory extends AbstractCodec.Factory<Beats2Codec> {
        @Override
        Beats2Codec create(Configuration configuration);

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
                    CK_NO_BEATS_PREFIX,
                    "Do not add Beats type as prefix",
                    false,
                    "Do not prefix each field with the Beats type, e. g. \"source\" -> \"filebeat_source\"."
            ));

            return configurationRequest;
        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(Beats2Codec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
