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
package org.graylog.storage.elasticsearch6.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog2.jackson.TypeReferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FixtureImporterES6 implements FixtureImporter {
    private static final Logger LOG = LoggerFactory.getLogger(FixtureImporter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JestClient jestClient;

    public FixtureImporterES6(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public void importResource(URL resource) {
        LOG.debug("Importing fixture resource: {}", resource);
        try {
            final JsonNode root = OBJECT_MAPPER.readValue(resource, JsonNode.class);
            importNode(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void importNode(JsonNode root) throws IOException {
        /* This supports the nosqlunit DataSet structure:
         *
         *  {
         *    "documents": [
         *      {
         *        "document": [
         *          {
         *            "index": {
         *              "indexName": "graylog_0",
         *              "indexType": "message",
         *              "indexId": "0"
         *            }
         *          },
         *          {
         *            "data": {
         *              "source": "example.org",
         *              "message": "Hi",
         *              "timestamp": "2015-01-01 01:00:00.000"
         *            }
         *          }
         *        ]
         *      }
         *    ]
         *  }
         */
        final Bulk.Builder bulkBuilder = new Bulk.Builder();

        final Set<String> indicesToCreate = new HashSet<>();
        for (final JsonNode document : root.path("documents")) {
            final List<JsonNode> indexes = new ArrayList<>();
            Map<String, Object> data = new HashMap<>();

            for (JsonNode entry : document.path("document")) {
                if (entry.hasNonNull("index")) {
                    indexes.add(entry.path("index"));
                } else if (entry.hasNonNull("data")) {
                    data = OBJECT_MAPPER.convertValue(entry.path("data"), TypeReferences.MAP_STRING_OBJECT);
                }
            }

            for (final JsonNode index : indexes) {
                final Index.Builder indexBuilder = new Index.Builder(data);

                if (!index.hasNonNull("indexName")) {
                    throw new IllegalArgumentException("Missing indexName in " + index);
                }

                final String indexName = index.path("indexName").asText();
                indicesToCreate.add(indexName);
                indexBuilder.index(indexName);

                if (index.hasNonNull("indexType")) {
                    indexBuilder.type(index.path("indexType").asText());
                }

                if (index.hasNonNull("indexId")) {
                    indexBuilder.id(index.path("indexId").asText());
                }

                bulkBuilder.addAction(indexBuilder.build());
            }
        }

        for (String indexName : indicesToCreate) {
            if (!indexExists(indexName)) {
                createIndex(indexName);
            }
        }

        final BulkResult result = jestClient.execute(bulkBuilder.build());
        if (!result.isSucceeded()) {
            final StringBuilder sb = new StringBuilder("Error while bulk indexing documents: ").append(result.getErrorMessage());

            if (!result.getFailedItems().isEmpty()) {
                sb.append('\n');
                result.getFailedItems().forEach(item -> {
                    final String s = MoreObjects.toStringHelper(BulkResult.BulkResultItem.class)
                            .omitNullValues()
                            .add("operation", item.operation)
                            .add("index", item.index)
                            .add("type", item.type)
                            .add("id", item.id)
                            .add("status", item.status)
                            .add("error", item.error)
                            .add("errorType", item.errorType)
                            .add("errorReason", item.errorReason)
                            .add("version", item.version)
                            .toString();
                    sb.append('\n').append(s);
                });
            }

            throw new IllegalStateException(sb.toString());
        }
    }

    private boolean indexExists(String indexName) throws IOException {
        final IndicesExists.Builder request = new IndicesExists.Builder(indexName);
        final JestResult result = jestClient.execute(request.build());

        return result.isSucceeded();
    }

    private void createIndex(String indexName) {
        final CreateIndex.Builder request = new CreateIndex.Builder(indexName);
        JestUtils.execute(jestClient, request.build(), () -> "Unable to create index for test: " + indexName);
    }
}
