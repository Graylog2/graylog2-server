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

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.graylog2.Tools;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexer.java: Sep 05, 2011 9:13:03 PM
 *
 * Stores/indexes log messages in ElasticSearch.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
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
    public static boolean indexExists() throws IOException {
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
    }

    /**
     * Creates the index including the mapping.
     *
     * http://www.elasticsearch.org/guide/reference/api/admin-indices-create-index.html
     * http://www.elasticsearch.org/guide/reference/mapping
     */
    public static boolean createIndex() throws IOException {
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
    }

    /**
     * Bulk-indexes/persists messages to ElasticSearch.
     * http://www.elasticsearch.org/guide/reference/api/bulk.html
     *
     * @param message The message to index.
     * @return
     */
    public static boolean bulkIndex(List<GELFMessage> messages) {
        if (messages.isEmpty()) {
            return true;
        }
        
        String batch = "";

        for (GELFMessage message : messages) {
            batch += "{\"index\":{\"_index\":\"" + INDEX + "\",\"_type\":\"" + TYPE + "\"}}\n";
            batch += JSONValue.toJSONString(message.toElasticSearchObject()) + "\n";
        }

        try {
            URL url = new URL("http://localhost:9200/_bulk");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(batch);
            writer.close();
            if (conn.getResponseCode() == 201) {
                return true;
            } else {
                LOG.warn("Indexer response code was not 201, but " + conn.getResponseCode());
                return false; 
            }
        } catch (IOException e) {
            LOG.warn("IO error when trying to index messages: " + e.getMessage(), e);
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
