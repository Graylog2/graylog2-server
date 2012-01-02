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
import org.graylog2.Main;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * ElasticSearchHttpIndexer.java: Sep 05, 2011 9:13:03 PM
 * <p/>
 * Stores/indexes log messages in ElasticSearch.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ElasticSearchHttpIndexer implements Indexer {

    // XXX ELASTIC: refactor.

    private static final Logger LOG = Logger.getLogger(ElasticSearchHttpIndexer.class);

    private String indexName;
    private String indexType;
    
    private HttpURLConnection connection = null;

    public ElasticSearchHttpIndexer(String indexName, String indexType) throws Exception {
        this.indexName = indexName;
        this.indexType = indexType;
        
        if(!indexExists()) {
            createIndex();
        }
    }

    /**
     * Checks if the index for Graylog2 exists
     * <p/>
     * See <a href="http://www.elasticsearch.org/guide/reference/api/admin-indices-indices-exists.html">elasticsearch Indices Exists API</a> for details.
     *
     * @return {@literal true} if the index for Graylog2 exists, {@literal false} otherwise
     * @throws IOException if elasticsearch server couldn't be reached
     */
    private boolean indexExists() throws IOException {
        URL url = new URL(buildIndexURL());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        // Older versions of ElasticSearch return 400 Bad Request in cse of an existing index.
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK || conn.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            return true;
        } else {
            if (conn.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
                LOG.warn("ElasticSearchHttpIndexer response code was not (200 or 400) or 404, but " + conn.getResponseCode());
            }

            return false;
        }
    }

    /**
     * Creates the index for Graylog2 including the mapping
     * <p/>
     * <a href="http://www.elasticsearch.org/guide/reference/api/admin-indices-create-index.html">Create Index API</a> and
     * <a href="http://www.elasticsearch.org/guide/reference/mapping">elasticsearch Mapping</a>
     *
     * @return {@literal true} if the index for Graylog2 could be created, {@literal false} otherwise
     * @throws IOException if elasticsearch server couldn't be reached
     */
    private boolean createIndex() throws IOException {

        Writer writer = null;
        URL url = new URL(buildIndexURL());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        try {
            writer = new OutputStreamWriter(conn.getOutputStream());

            // Write Mapping.
            writer.write(JSONValue.toJSONString(Mapping.get()));
            writer.flush();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                LOG.warn("Response code of create index operation was not 200, but " + conn.getResponseCode());
                return false;
            }
        } finally {
            if (null != writer) {
                writer.close();
            }
        }
    }

    /**
     * Bulk-indexes/persists messages to ElasticSearch.
     * <p/>
     * See <a href="http://www.elasticsearch.org/guide/reference/api/bulk.html">elasticsearch Bulk API</a> for details
     *
     * @param messages The messages to index
     * @return {@literal true} if the messages were successfully indexed, {@literal false} otherwise
     */
    public boolean bulkIndex(List<GELFMessage> messages) {

        if (messages.isEmpty()) {
            return true;
        }

        Writer writer = null;
        int responseCode = 0;

        try {

            if(connection == null) {
                connection = openConnection();
            }
            
            connection.connect();

            writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(getJSONfromGELFMessages(messages));
            writer.flush();

            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            LOG.warn("IO error when trying to index messages", e);
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOG.error("Couldn't close output stream", e);
                }
            }
        }

        if (responseCode == HttpURLConnection.HTTP_OK) {
            return true;
        } else {
            LOG.warn("ElasticSearchHttpIndexer response code was not 200, but " + responseCode);
            return false;
        }
    }

    private HttpURLConnection openConnection() throws IOException {

        URL url = new URL(buildElasticSearchURL() + "_bulk");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        return connection;
    }

    /**
     * Deletes all messages from index which are older than the specified timestamp.
     *
     * @param to UTC UNIX timestamp
     * @return {@literal true} if the messages were successfully deleted, {@literal false} otherwise
     */
    public boolean deleteMessagesByTimeRange(int to) {
        int responseCode = 0;

        try {
            URL url = new URL(buildIndexWithTypeUrl() + buildDeleteByQuerySinceDate(to));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.connect();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            LOG.warn("IO error when trying to delete messages older than date", e);
        } 

        if (responseCode == HttpURLConnection.HTTP_OK) {
            return true;
        } else {
            LOG.warn("ElasticSearchHttpIndexer response code was not 200, but " + responseCode);
            return false;
        }
    }

    private static String buildDeleteByQuerySinceDate(int to) {
        return "/_query?q=created_at%3A%5B0%20TO%20" + to + "%5D";
    }

    private String getJSONfromGELFMessages(List<GELFMessage> messages) {
        StringBuilder sb = new StringBuilder();

        for (GELFMessage message : messages) {
            sb.append("{\"index\":{\"_index\":\"");
            sb.append(indexName);
            sb.append("\",\"_type\":\"");
            sb.append(indexType);
            sb.append("\"}}\n");
            sb.append(JSONValue.toJSONString(message.toElasticSearchObject()));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String buildElasticSearchURL() {
        return Main.configuration.getElasticSearchUrl();
    }

    private String buildIndexURL() {
        return buildElasticSearchURL() + indexName;
    }

    private String buildIndexWithTypeUrl() {
        return buildIndexURL() + "/" + indexType;
    }

}
