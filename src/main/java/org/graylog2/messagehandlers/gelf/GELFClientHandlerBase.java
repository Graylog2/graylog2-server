/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.gelf;

import org.apache.log4j.Logger;
import org.graylog2.messagehandlers.syslog.SyslogEventHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.Iterator;
import java.util.Set;

/**
 * GELFClient.java: Sep 14, 2010 6:43:00 PM
 *
 * GELF Client base class. Has all the methods we need for
 * converting, decoding, etc.
 *
 * Shared by Chunked/SimpleGELFClient
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
class GELFClientHandlerBase {

    private static final Logger LOG = Logger.getLogger(SyslogEventHandler.class);

    protected String clientMessage = null;
    protected GELFMessage message = new GELFMessage();

    protected GELFClientHandlerBase() { }

    protected boolean parse() throws Exception{
        JSONObject json = this.getJSON(this.clientMessage.toString());
        if (json == null) {
            LOG.warn("JSON is null/could not be parsed (invalid JSON) - clientMessage was: " + this.clientMessage);
            return false;
        }

        // Add standard fields.
        this.message.setVersion(this.jsonToString(json.get("version")));
        this.message.setHost(this.jsonToString(json.get("host")));
        this.message.setShortMessage(this.jsonToString(json.get("short_message")));
        this.message.setFullMessage(this.jsonToString(json.get("full_message")));
        this.message.setFile(this.jsonToString(json.get("file")));
        this.message.setLine(this.jsonToInt(json.get("line")));

        // Level is set by server if not specified by client.
        int level = this.jsonToInt(json.get("level"));
        if (level > -1) {
            this.message.setLevel(level);
        } else {
            this.message.setLevel(GELF.STANDARD_LEVEL_VALUE);
        }

        // Facility is set by server if not specified by client.
        String facility = this.jsonToString(json.get("facility"));
        if (facility == null) {
            this.message.setFacility(GELF.STANDARD_FACILITY_VALUE);
        } else {
            this.message.setFacility(facility);
        }

        // Add additional data if there is some.
        Set<String> set = json.keySet();
        Iterator<String> iter = set.iterator();
        while(iter.hasNext()) {
            String key = iter.next();

            // Skip standard fields.
            if (!key.startsWith(GELF.USER_DEFINED_FIELD_PREFIX)) {
                continue;
            }

            // Don'T allow to override _id. (just to make sure...)
            if (key.equals("_id")) {
                LOG.warn("Client tried to override _id field! Skipped field, but still storing message.");
                continue;
            }

            // Add to message.
            this.message.addAdditionalData(key, this.jsonToString(json.get(key)));
        }

        return true;
    }

    protected JSONObject getJSON(String value) throws Exception {
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

    protected String getClientMessage() {
        return clientMessage;
    }
}
