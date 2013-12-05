package org.graylog2.inputs.gelf.gelf;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
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
public class GELFParser {
    private static final Logger LOG = LoggerFactory.getLogger(GELFProcessor.class);

    protected final ObjectMapper objectMapper;
    protected InputHost server;

    public GELFParser(InputHost server) {
        objectMapper = new ObjectMapper();
        this.server = server;
    }

    protected Message parse(String message, MessageInput sourceInput) {
        Timer.Context tcx = server.metrics().timer(name(sourceInput.getUniqueReadableId(), "gelfParsedTime")).time();

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
        double messageTimestamp = doubleValue(json, "timestamp");
        DateTime timestamp;
        if (messageTimestamp <= 0) {
            timestamp = new DateTime();
        } else {
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
            } else {
                fieldValue = value.asText();
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
