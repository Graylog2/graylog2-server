/*
 * Copyright 2013-2014 TORCH GmbH
 *
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
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.inputs.gelf.gelf;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.inputs.MessageInput;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GELFProcessor.class);
    private InputHost server;

    private final ObjectMapper objectMapper;

    public GELFProcessor(InputHost server) {
        this.server = server;

        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    public void messageReceived(GELFMessage message, MessageInput sourceInput) throws BufferOutOfCapacityException {
        String metricName = sourceInput.getUniqueReadableId();

        server.metrics().meter(name(metricName, "incomingMessages")).mark();

        // Convert to LogMessage
        Message lm = null;
        try {
            lm = parse(message.getJSON(), sourceInput);
        } catch (IllegalStateException e) {
            LOG.error("Corrupt or invalid message received: ", e);
            return;
        }

        if (lm == null || !lm.isComplete()) {
            server.metrics().meter(name(metricName, "incompleteMessages")).mark();
            LOG.debug("Skipping incomplete message: {}", lm.getValidationErrors());
            return;
        }

        // Add to process buffer.
        LOG.debug("Adding received GELF message <{}> to process buffer: {}", lm.getId(), lm);
        server.metrics().meter(name(metricName, "processedMessages")).mark();
        server.getProcessBuffer().insertCached(lm, sourceInput);
    }

    private Message parse(String message, MessageInput sourceInput) {
        Timer.Context tcx = server.metrics().timer(name(sourceInput.getUniqueReadableId(), "gelfParsedTime")).time();

        JsonNode json;

        try {
            json = objectMapper.readTree(message);
        } catch (Exception e) {
            LOG.error("Could not parse JSON!", e);
            LOG.debug("This is the failed message: ", message);
            json = null;
        }

        if (json == null) {
            throw new IllegalStateException("JSON is null/could not be parsed (invalid JSON)");
        }

        // Timestamp.
        double messageTimestamp = doubleValue(json, "timestamp");
        DateTime timestamp;
        if (messageTimestamp <= 0) {
            timestamp = Tools.iso8601();
        } else {
            // we treat this as a unix timestamp
            timestamp = Tools.dateTimeFromDouble(messageTimestamp);
        }

        Message lm = new Message(
        		this.stringValue(json, "short_message"),
        		this.stringValue(json, "host"),
        		timestamp
        );

        lm.addField("full_message", this.stringValue(json, "full_message"));

        String file = this.stringValue(json, "file");

        if (file != null && !file.isEmpty()) {
        	lm.addField("file", file);
        }

        long line = this.longValue(json, "line");
        if (line > -1) {
        	lm.addField("line", line);
        }

        // Level is set by server if not specified by client.
        long level = this.longValue(json, "level");
        if (level > -1) {
            lm.addField("level", level);
        }

        // Facility is set by server if not specified by client.
        String facility = this.stringValue(json, ("facility"));
        if (facility != null && !facility.isEmpty()) {
            lm.addField("facility", facility);
        }

        // Add additional data if there is some.
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();

        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();

            String key = entry.getKey();
            JsonNode value = entry.getValue();

            // Don't include GELF syntax underscore in message field key.
            if (key.startsWith("_") && key.length() > 1) {
                key = key.substring(1);
            }

            // We already set short_message and host as message and source. Do not add as fields again.
            if (key.equals("short_message") || key.equals("host")) {
                continue;
            }

            // Skip standard or already set fields.
            if (lm.getField(key) != null || Message.RESERVED_FIELDS.contains(key)) {
                continue;
            }

            // Convert JSON containers to Strings, and pick a suitable number representation.
            Object fieldValue;
            if (value.isContainerNode()) {
                fieldValue = value.toString();
            } else if (value.isFloatingPointNumber()) {
                fieldValue = value.asDouble();
            } else if (value.isIntegralNumber()) {
                fieldValue = value.asLong();
            } else if (value.isNull()) {
                LOG.debug("Field [{}] is NULL. Skipping.", key);
                continue;
            } else if(value.isTextual()) {
                fieldValue = value.asText();
            } else {
                LOG.debug("Field [{}] has unknown value type. Skipping.", key);
                continue;
            }

            lm.addField(key, fieldValue);
        }

        // Stop metrics timer.
        tcx.stop();

        return lm;
    }

    private String stringValue(JsonNode json, String fieldName) {
        if (json != null) {
            JsonNode value = json.get(fieldName);

            if (value != null) {
                return value.asText();
            }
        }

        return null;
    }

    private long longValue(JsonNode json, String fieldName) {
        if (json != null) {
            JsonNode value = json.get(fieldName);

            if (value != null) {
                return value.asLong(-1L);
            }
        }

        return -1L;
    }

    private double doubleValue(JsonNode json, String fieldName) {
        if (json != null) {
            JsonNode value = json.get(fieldName);

            if (value != null) {
                return value.asDouble(-1.0);
            }
        }

        return -1.0;
    }
}
