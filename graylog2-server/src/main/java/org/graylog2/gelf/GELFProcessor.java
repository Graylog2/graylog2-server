/**
 * Copyright 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2.gelf;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.Core;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
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
    private Core server;

    private final Meter incomingMessages;
    private final Meter incompleteMessages;
    private final Meter processedMessages;
    private final Timer gelfParsedTime;

    private final ObjectMapper objectMapper;

    public GELFProcessor(Core server) {
        this.server = server;

        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

        this.incomingMessages = server.metrics().meter(name(GELFProcessor.class, "incomingMessages"));
        this.incompleteMessages = server.metrics().meter(name(GELFProcessor.class, "incompleteMessages"));
        this.processedMessages = server.metrics().meter(name(GELFProcessor.class, "processedMessages"));
        this.gelfParsedTime = server.metrics().timer(name(GELFProcessor.class, "gelfParsedTime"));
    }

    public void messageReceived(GELFMessage message) throws BufferOutOfCapacityException {
        incomingMessages.mark();
        
        // Convert to LogMessage
        Message lm = parse(message.getJSON());

        if (!lm.isComplete()) {
            incompleteMessages.mark();
            LOG.debug("Skipping incomplete message.");
        }

        // Add to process buffer.
        LOG.debug("Adding received GELF message <{}> to process buffer: {}", lm.getId(), lm);
        processedMessages.mark();
        server.getProcessBuffer().insertCached(lm);
    }

    private Message parse(String message) {
        Timer.Context tcx = gelfParsedTime.time();

        JsonNode json;

        try {
            json = objectMapper.readTree(message);
        } catch (Exception e) {
            LOG.error("Could not parse JSON!", e);
            json = null;
        }

        if (json == null) {
            throw new IllegalStateException("JSON is null/could not be parsed (invalid JSON)");
        }
        
        // Timestamp.
        double timestamp = doubleValue(json, "timestamp");
        if (timestamp <= 0) {
            timestamp = Tools.getUTCTimestampWithMilliseconds();
        }
        
        Message lm = new Message(
        		this.stringValue(json, "short_message"),
        		this.stringValue(json, "host"),
        		timestamp);
        
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

            // Skip standard fields.
            if (null != lm.getField(key) || Message.RESERVED_FIELDS.contains(key)) {
                continue;
            }

            // Convert JSON containers to Strings.
            if (value.isContainerNode()) {
                lm.addField(key, value.toString());
            } else {
                lm.addField(key, value.asText());
            }
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
