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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
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
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Codec(name = "beats-deprecated", displayName = "Beats (deprecated)")
public class BeatsCodec extends AbstractCodec {
    private static final Logger LOG = LoggerFactory.getLogger(BeatsCodec.class);
    private static final String MAP_KEY_SEPARATOR = "_";

    private final ObjectMapper objectMapper;

    @Inject
    public BeatsCodec(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        super(configuration);
        this.objectMapper = requireNonNull(objectMapper);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        final byte[] payload = rawMessage.getPayload();
        final Map<String, Object> event;
        try {
            event = objectMapper.readValue(payload, TypeReferences.MAP_STRING_OBJECT);
        } catch (IOException e) {
            LOG.error("Couldn't decode raw message {}", rawMessage);
            return null;
        }

        return parseEvent(event);
    }

    @Nullable
    private Message parseEvent(Map<String, Object> event) {
        @SuppressWarnings("unchecked")
        final Map<String, String> metadata = (HashMap<String, String>) event.remove("@metadata");
        final String type;
        if (metadata == null) {
            LOG.warn("Couldn't recognize Beats type");
            type = "unknown";
        } else {
            type = metadata.get("beat");
        }
        final Message gelfMessage;
        switch (type) {
            case "filebeat":
                gelfMessage = parseFilebeat(event);
                break;
            case "topbeat":
                gelfMessage = parseTopbeat(event);
                break;
            case "metricbeat":
                gelfMessage = parseMetricbeat(event);
                break;
            case "packetbeat":
                gelfMessage = parsePacketbeat(event);
                break;
            case "winlogbeat":
                gelfMessage = parseWinlogbeat(event);
                break;
            default:
                LOG.debug("Unknown beats type {}. Using generic handler.", type);
                gelfMessage = parseGenericBeat(event);
                break;
        }

        return gelfMessage;
    }

    private Message createMessage(String message, Map<String, Object> event) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> beat = (Map<String, Object>) event.remove("beat");
        final String hostname;
        final String name;
        if (beat == null) {
            hostname = "unknown";
            name = "unknown";
        } else {
            hostname = String.valueOf(beat.get("hostname"));
            name = String.valueOf(beat.get("name"));
        }
        final String timestampField = String.valueOf(event.remove("@timestamp"));
        final DateTime timestamp = Tools.dateTimeFromString(timestampField);
        final String type = String.valueOf(event.get("type"));
        final Object tags = event.get("tags");

        final Message result = new Message(message, hostname, timestamp);
        result.addField("name", name);
        result.addField("type", type);
        result.addField("tags", tags);

        @SuppressWarnings("unchecked")
        final Map<String, Object> fields = (Map<String, Object>) event.get("fields");
        if (fields != null) {
            result.addFields(fields);
        }

        return result;
    }

    /**
     * @see <a href="https://www.elastic.co/guide/en/beats/filebeat/1.2/exported-fields.html">Filebeat Exported Fields</a>
     */
    private Message parseFilebeat(Map<String, Object> event) {
        final String message = String.valueOf(event.get("message"));
        final Message gelfMessage = createMessage(message, event);
        gelfMessage.addField("facility", "filebeat");
        gelfMessage.addField("file", event.get("source"));
        gelfMessage.addField("input_type", event.get("input_type"));
        gelfMessage.addField("count", event.get("count"));
        gelfMessage.addField("offset", event.get("offset"));

        return gelfMessage;
    }

    /**
     * @see <a href="https://www.elastic.co/guide/en/beats/topbeat/1.2/exported-fields.html">Topbeat Exported Fields</a>
     */
    private Message parseTopbeat(Map<String, Object> event) {
        final Message gelfMessage = createMessage("-", event);
        gelfMessage.addField("facility", "topbeat");
        final Map<String, Object> flattened = MapUtils.flatten(event, "topbeat", MAP_KEY_SEPARATOR);

        // Fix field names containing dots, like "cpu.name"
        final Map<String, Object> withoutDots = MapUtils.replaceKeyCharacter(flattened, '.', MAP_KEY_SEPARATOR.charAt(0));
        gelfMessage.addFields(withoutDots);
        return gelfMessage;
    }

    /**
     * @see <a href="https://www.elastic.co/guide/en/beats/metricbeat/5.1/exported-fields.html">Metricbeat Exported Fields</a>
     */
    private Message parseMetricbeat(Map<String, Object> event) {
        final Message gelfMessage = createMessage("-", event);
        gelfMessage.addField("facility", "metricbeat");
        final Map<String, Object> flattened = MapUtils.flatten(event, "metricbeat", MAP_KEY_SEPARATOR);

        // Fix field names containing dots, like "cpu.name"
        final Map<String, Object> withoutDots = MapUtils.replaceKeyCharacter(flattened, '.', MAP_KEY_SEPARATOR.charAt(0));
        gelfMessage.addFields(withoutDots);
        return gelfMessage;
    }

    /**
     * @see <a href="https://www.elastic.co/guide/en/beats/packetbeat/1.2/exported-fields.html">Packetbeat Exported Fields</a>
     */
    private Message parsePacketbeat(Map<String, Object> event) {
        final Message gelfMessage = createMessage("-", event);
        gelfMessage.addField("facility", "packetbeat");
        final Map<String, Object> flattened = MapUtils.flatten(event, "packetbeat", MAP_KEY_SEPARATOR);

        // Fix field names containing dots, like "icmp.version"
        final Map<String, Object> withoutDots = MapUtils.replaceKeyCharacter(flattened, '.', MAP_KEY_SEPARATOR.charAt(0));
        gelfMessage.addFields(withoutDots);

        return gelfMessage;
    }

    /**
     * @see <a href="https://www.elastic.co/guide/en/beats/winlogbeat/1.2/exported-fields.html">Winlogbeat Exported Fields</a>
     */
    private Message parseWinlogbeat(Map<String, Object> event) {
        final String message = String.valueOf(event.remove("message"));
        final Message gelfMessage = createMessage(message, event);
        gelfMessage.addField("facility", "winlogbeat");
        final Map<String, Object> flattened = MapUtils.flatten(event, "winlogbeat", MAP_KEY_SEPARATOR);

        // Fix field names containing dots, like "user.name"
        final Map<String, Object> withoutDots = MapUtils.replaceKeyCharacter(flattened, '.', MAP_KEY_SEPARATOR.charAt(0));
        gelfMessage.addFields(withoutDots);
        return gelfMessage;
    }

    private Message parseGenericBeat(Map<String, Object> event) {
        final String message = String.valueOf(event.remove("message"));
        final Message gelfMessage = createMessage(message, event);
        gelfMessage.addField("facility", "genericbeat");
        final Map<String, Object> flattened = MapUtils.flatten(event, "beat", MAP_KEY_SEPARATOR);

        // Fix field names containing dots
        final Map<String, Object> withoutDots = MapUtils.replaceKeyCharacter(flattened, '.', MAP_KEY_SEPARATOR.charAt(0));
        gelfMessage.addFields(withoutDots);
        return gelfMessage;
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
    }


    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(BeatsCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
