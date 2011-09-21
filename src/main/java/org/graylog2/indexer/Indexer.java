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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
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

    private static final Logger LOG = Logger.getLogger(Indexer.class);

    public static final String INDEX = "graylog2";
    public static final String TYPE = "message";
    
    public static void index(GELFMessage message) {
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

        ///// TODO: obj.put("streams", message.getStreamIds());

        try {
            URL url = new URL(Indexer.buildTargetUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(JSONValue.toJSONString(obj));
            writer.close();

            // TODO: REMOVE
            String response = IOUtils.toString(conn.getInputStream());
        } catch (IOException e) {
            LOG.warn("IO error when trying to index message: " + e.getMessage(), e);
        }
    }

    private static String buildTargetUrl() {
        return "http://localhost:9200/" + Indexer.INDEX + "/" + Indexer.TYPE;
    }

}