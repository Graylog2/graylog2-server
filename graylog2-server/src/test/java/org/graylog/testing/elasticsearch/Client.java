/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.testing.elasticsearch;

import java.util.Map;

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

    void removeAliasMapping(String indexName, String alias);

    boolean templateExists(String templateName);

    void putTemplate(String templateName, Map<String, Object> source);

    void deleteTemplates(String... templates);

    void waitForGreenStatus(String... indices);

    void refreshNode();

    void bulkIndex(BulkIndexRequest bulkIndexRequest);

    void cleanUp();

    String fieldType(String testIndexName, String source);

    void putSetting(String setting, String value);

    void waitForIndexBlock(String index);

    void resetIndexBlock(String index);
}
