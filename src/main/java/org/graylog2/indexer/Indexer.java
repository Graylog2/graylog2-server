/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.indexer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.graylog2.Tools;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.json.simple.JSONValue;

/**
 * Indexer.java: Sep 05, 2011 9:13:03 PM
 *
 * Stores/indexes log messages in ElasticSearch.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class Indexer {

    // XXX ELASTIC: refactor.

    private static final Logger LOG = Logger.getLogger(Indexer.class);

    public static final String INDEX = "graylog2";
    public static final String TYPE = "message";
    
    /**
     * Checks if the index exists.
     * 
     * http://www.elasticsearch.org/guide/reference/api/admin-indices-indices-exists.html
     */
    public static boolean indexExists() {
        try {
            URL url = new URL(Indexer.buildIndexURL());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            if (conn.getResponseCode() == 200) {
                return true;
            } else {
                if (conn.getResponseCode() != 404) {
                    LOG.warn("Indexer response code was not 200 or 404, but " + conn.getResponseCode());
                }
                
                return false;
            }
        } catch (IOException e) {
            LOG.warn("IO error when trying to check if index exists: " + e.getMessage(), e);
        }

        return false;
    }

    /**
     * Creates the index including the mapping.
     *
     * http://www.elasticsearch.org/guide/reference/api/admin-indices-create-index.html
     * http://www.elasticsearch.org/guide/reference/mapping
     */
    public static boolean createIndex() {
        try {
            URL url = new URL(Indexer.buildIndexURL());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            // Write Mapping.
            writer.write(JSONValue.toJSONString(Mapping.get()));
            writer.close();
            if (conn.getResponseCode() == 200) {
                return true;
            } else {
                LOG.warn("Response code of create index operation was not 201, but " + conn.getResponseCode());
                return false;
            }
        } catch (IOException e) {
            LOG.warn("IO error when trying to create index: " + e.getMessage(), e);
        }

        return false;
    }

    /**
     * Indexes a message.
     *
     * @param message The message to index.
     * @return
     */
    public static boolean index(GELFMessage message) {
        Map obj = new HashMap();
        obj.put("message", message.getShortMessage());
        obj.put("full_message", message.getFullMessage());
        obj.put("file", message.getFile());
        obj.put("line", message.getLine());
        obj.put("host", message.getHost());
        obj.put("facility", message.getFacility());
        obj.put("level", message.getLevel());

        // Add additional fields. XXX PERFORMANCE
        Map<String,Object> additionalFields = message.getAdditionalData();
        Set<String> set = additionalFields.keySet();
        Iterator<String> iter = set.iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            Object value = additionalFields.get(key);
            obj.put(key, value);
        }

        if (message.getCreatedAt() <= 0) {
            // This should have already been set at receiving, but to make sure...
            obj.put("created_at", Tools.getUTCTimestampWithMilliseconds());
        } else {
            obj.put("created_at", message.getCreatedAt());
        }

        ////// XXX ELASTIC: required to manually convert to string? caused strange problems without it.
        List<String> streamIds = new ArrayList<String>();
        for (ObjectId id : message.getStreamIds()) {
            streamIds.add(id.toString());
        }

        obj.put("streams", streamIds);
        ///////////////

        try {
            URL url = new URL(Indexer.buildIndexWithTypeUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(JSONValue.toJSONString(obj));
            writer.close();
            if (conn.getResponseCode() == 201) {
                return true;
            } else {
                LOG.warn("Indexer response code was not 201, but " + conn.getResponseCode());
                return false; 
            }
        } catch (IOException e) {
            LOG.warn("IO error when trying to index message: " + e.getMessage(), e);
        }

        // Not reached.
        return false;
    }

    private static String buildIndexURL() {
        return "http://localhost:9200/" + Indexer.INDEX;
    }

    private static String buildIndexWithTypeUrl() {
        return buildIndexURL() + "/" + Indexer.TYPE;
    }

}
