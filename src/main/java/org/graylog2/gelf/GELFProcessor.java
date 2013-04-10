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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.graylog2.Core;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GELFProcessor.class);
    private Core server;
    private final Meter incomingMessages = Metrics.newMeter(GELFProcessor.class, "IncomingMessages", "messages", TimeUnit.SECONDS);
    private final Meter incompleteMessages = Metrics.newMeter(GELFProcessor.class, "IncompleteMessages", "messages", TimeUnit.SECONDS);
    private final Meter processedMessages = Metrics.newMeter(GELFProcessor.class, "ProcessedMessages", "messages", TimeUnit.SECONDS);
    private final Timer gelfParsedTime = Metrics.newTimer(GELFProcessor.class, "GELFParsedTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    public GELFProcessor(Core server) {
        this.server = server;
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
        TimerContext tcx = gelfParsedTime.time();

        JSONObject json;

        try {
            json = getJSON(message);
        } catch (Exception e) {
            LOG.error("Could not parse JSON!", e);
            json = null;
        }

        if (json == null) {
            throw new IllegalStateException("JSON is null/could not be parsed (invalid JSON)");
        }
        
        // Timestamp.
        double timestamp = this.jsonToDouble(json.get("timestamp"));
        if (timestamp <= 0) {
            timestamp = Tools.getUTCTimestampWithMilliseconds();
        }
        
        Message lm = new Message(
        		this.jsonToString(json.get("short_message")),
        		this.jsonToString(json.get("host")),
        		timestamp);
        
        lm.addField("full_message", this.jsonToString(json.get("full_message")));
        
        String file = this.jsonToString(json.get("file"));
        if (file != null && !file.isEmpty()) {
        	lm.addField("file", file);
        }

        int line = this.jsonToInt(json.get("line"));
        if (line > -1) {
        	lm.addField("line", line);
        }
        
        // Level is set by server if not specified by client.
        int level = this.jsonToInt(json.get("level"));
        if (level > -1) {
            lm.addField("level", level);
        }

        // Facility is set by server if not specified by client.
        String facility = this.jsonToString(json.get("facility"));
        if (facility != null && !facility.isEmpty()) {
            lm.addField("facility", facility);
        }

        // Add additional data if there is some.
        @SuppressWarnings("unchecked")
		Set<Map.Entry<String, Object>> entrySet = json.entrySet();
        for(Map.Entry<String, Object> entry : entrySet) {

            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip standard fields.
            if (Message.RESERVED_FIELDS.contains(key)) {
                continue;
            }
            
            // Convert JSON containers to Strings.
            if (value instanceof Map || value instanceof Set || value instanceof List) {
                value = value.toString();
            }

            // Add to message.
            lm.addField(key, value);
        }

        // Stop metrics timer.
        tcx.stop();
        
        return lm;
    }

    private JSONObject getJSON(String value) {
        if (value != null) {
            Object obj = JSONValue.parse(value);
            if (obj != null) {
                if (obj instanceof org.json.simple.JSONArray) {
                    // Return the k/v of ths JSON array if this is an array.
                    JSONArray array = (JSONArray)obj;
                    return (JSONObject) array.get(0);
                } else if(obj instanceof org.json.simple.JSONObject) {
                    // This is not an array. Convert it to an JSONObject directly without choosing first k/v.
                    return (JSONObject)obj;
                }
            }
        }

        return null;
    }

    private String jsonToString(Object json) {
        try {
            if (json != null) {
                return json.toString();
            }
        } catch(Exception e) {}

        return null;
    }

    private int jsonToInt(Object json) {
        try {
            if (json != null) {
                String str = this.jsonToString(json);
                if (str != null) {
                    return Integer.parseInt(str);
                }
            }
        } catch(Exception e) {}

        return -1;
    }

    private double jsonToDouble(Object json) {
        try {
            if (json != null) {
                String str = this.jsonToString(json);
                if (str != null) {
                    return Double.parseDouble(str);
                }
            }
        } catch(Exception e) {}

        return -1;
    }

}
