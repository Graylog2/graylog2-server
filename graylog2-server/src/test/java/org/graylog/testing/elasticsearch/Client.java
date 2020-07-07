/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.testing.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;

public interface Client {
    default void createIndex(String index) {
        createIndex(index, 1, 0);
    }

    void createIndex(String index, int shards, int replicas);

    default String createRandomIndex(String prefix) {
        final String indexName = prefix + System.nanoTime();

        createIndex(indexName);
        waitForGreenStatus(indexName);

        return indexName;
    }

    void deleteIndices(String... indices);

    void closeIndex(String index);

    boolean indicesExists(String... indices);

    void addAliasMapping(String indexName, String alias);

    JsonNode getMapping(String... indices);

    JsonNode getTemplate(String templateName);

    default JsonNode getTemplates() {
        return getTemplate("");
    }

    void putTemplate(String templateName, Object source);

    void deleteTemplates(String... templates);

    void waitForGreenStatus(String... indices);

    void refreshNode();

    void bulkIndex(BulkIndexRequest bulkIndexRequest);

    void cleanUp();
}
