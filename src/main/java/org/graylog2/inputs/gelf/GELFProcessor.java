/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.inputs.gelf;

import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.Tools;
import org.graylog2.logmessage.LogMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * GELFProcessor.java: 12.04.2012 10:59:57
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFProcessor {

    private static final Logger LOG = Logger.getLogger(GELFProcessor.class);
    private GraylogServer server;

    public GELFProcessor(GraylogServer server) {
        this.server = server;
    }

    public void messageReceived(GELFMessage message) throws Exception {
        // Convert to LogMessage
        LogMessage lm = parse(message.getJSON());

        if (!lm.isComplete()) {
            LOG.debug("Skipping incomplete message.");
        }

        // Add to process buffer.
        LOG.debug("Adding received GELF message <" + lm.getId() +"> to process buffer: " + lm);
        server.getProcessBuffer().insert(lm);
    }

    private LogMessage parse(String message) throws Exception {
        JSONObject json;
        LogMessage lm = new LogMessage();
        
        try {
            json = getJSON(message);
        } catch (Exception e) {
            LOG.error("Could not parse JSON!", e);
            json = null;
        }

        if (json == null) {
            throw new Exception("JSON is null/could not be parsed (invalid JSON)");
        }

        // Add standard fields.
        lm.setHost(this.jsonToString(json.get("host")));
        lm.setShortMessage(this.jsonToString(json.get("short_message")));
        lm.setFullMessage(this.jsonToString(json.get("full_message")));
        lm.setFile(this.jsonToString(json.get("file")));
        lm.setLine(this.jsonToInt(json.get("line")));

        // Level is set by server if not specified by client.
        int level = this.jsonToInt(json.get("level"));
        if (level > -1) {
            lm.setLevel(level);
        } else {
            lm.setLevel(LogMessage.STANDARD_LEVEL);
        }

        // Facility is set by server if not specified by client.
        String facility = this.jsonToString(json.get("facility"));
        if (facility == null) {
            lm.setFacility(LogMessage.STANDARD_FACILITY);
        } else {
            lm.setFacility(facility);
        }

        // Set createdAt to provided timestamp - Set to current time if not set.
        double timestamp = this.jsonToDouble(json.get("timestamp"));
        if (timestamp <= 0) {
            lm.setCreatedAt(Tools.getUTCTimestampWithMilliseconds());
        } else {
            lm.setCreatedAt(timestamp);
        }

        // Add additional data if there is some.
        Set<Map.Entry<String, String>> entrySet = json.entrySet();
        for(Map.Entry<String, String> entry : entrySet) {

            String key = entry.getKey();

            // Skip standard fields.
            if (!key.startsWith(GELFMessage.ADDITIONAL_FIELD_PREFIX)) {
                continue;
            }

            // Don't allow to override _id. (just to make sure...)
            if (key.equals("_id")) {
                LOG.warn("Client tried to override _id field! Skipped field, but still storing message.");
                continue;
            }

            // Add to message.
            lm.addAdditionalData(key, entry.getValue());
        }
        
        return lm;
    }

    private JSONObject getJSON(String value) {
        if (value != null) {
            Object obj = JSONValue.parse(value);
            if (obj != null) {
                if (obj.getClass().toString().equals("class org.json.simple.JSONArray")) {
                    // Return the k/v of ths JSON array if this is an array.
                    JSONArray array = (JSONArray)obj;
                    return (JSONObject) array.get(0);
                } else if(obj.getClass().toString().equals("class org.json.simple.JSONObject")) {
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
