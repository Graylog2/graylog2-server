/**
 * Copyright 2010 Lennart Koopmann <lennart@scopeport.org>
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

/**
 * GELFClient.java: Lennart Koopmann <lennart@scopeport.org> | Jun 23, 2010 7:15:12 PM
 */

package org.graylog2.messagehandlers.gelf;

import org.graylog2.Log;
import org.graylog2.Main;
import org.graylog2.database.MongoBridge;

import org.json.simple.*;

public class GELFClient {

    private String clientMessage = null;

    private GELFMessage message = new GELFMessage();

    public GELFClient(String clientMessage, String threadName) {
        this.clientMessage = clientMessage;
    }

    public boolean isValidAndJSON() {
        if(!this.clientMessage.contains("{")) {
            return false;
        }
        return true;
    }

    public boolean handle() {
        // Do a quick check if this could be valid JSON.
        /*if (!this.isValidAndJSON()) {
            Log.info("Got invalid GELF message: " + this.clientMessage);
            return false;
        }*/

        try {
            JSONObject json = this.getJSON(this.clientMessage);
            if (json == null) {
                Log.warn("JSON is null - clientMessage was: " + this.clientMessage);
                return false;
            }

            // Fills properties with values from JSON.
            try { this.parse(json); } catch(Exception e) {
                Log.warn("Could not parse GELF JSON: " + e.toString() + " - clientMessage was: " + this.clientMessage);
                return false;
            }

            // Store in MongoDB.
            // Connect to database.
            MongoBridge m = new MongoBridge(
                    Main.masterConfig.getProperty("mongodb_user"),
                    Main.masterConfig.getProperty("mongodb_password"),
                    Main.masterConfig.getProperty("mongodb_host"),
                    Main.masterConfig.getProperty("mongodb_database"),
                    Integer.valueOf(Main.masterConfig.getProperty("mongodb_port"))
            );


            // Log if we are in debug mode.
            Log.info("Got GELF message: \n" + message.toString());

            // Insert message into MongoDB.
            m.insertGelfMessage(message);
        } catch(Exception e) {
            Log.warn("Could not handle GELF client: " + e.getMessage());
            return false;
        }

        return true;
    }

    private void parse(JSONObject json) throws Exception{
        this.message.shortMessage = this.jsonToString(json.get("short_message"));
        this.message.fullMessage = this.jsonToString(json.get("full_message"));
        this.message.level = this.jsonToInt(json.get("level"));
        this.message.type = this.jsonToInt(json.get("type"));
        this.message.host = this.jsonToString(json.get("host"));
        this.message.file = this.jsonToString(json.get("file"));
        this.message.line = this.jsonToInt(json.get("line"));
    }

    private JSONObject getJSON(String value) {
        try {
            Object obj=JSONValue.parse(value);
            if (obj.getClass().toString().equals("class org.json.simple.JSONArray")) {
                // Return the k/v of ths JSON array if this is an array.
                JSONArray array=(JSONArray)obj;
                return (JSONObject) array.get(0);
            } else if(obj.getClass().toString().equals("class org.json.simple.JSONObject")) {
                // This is not an array. Convert it to an JSONObject directly without choosing first k/v.
                return (JSONObject)obj;
            }
        } catch(Exception e) {
            Log.warn("ScopeportClient::getJSON() failed: " + e.toString() + " - Tried to convert: " + value);
        }

        return new JSONObject();
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

        return 0;
    }

}
