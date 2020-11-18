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
package org.graylog.storage.elasticsearch7.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.index.IndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.ActiveShardCount;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.CreateIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetIndexRequest;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
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

public class FixtureImporterES7 implements FixtureImporter {
    private static final Logger LOG = LoggerFactory.getLogger(FixtureImporter.class);
    private final ElasticsearchClient client;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public FixtureImporterES7(ElasticsearchClient client) {
        this.client = client;
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
        final BulkRequest bulkRequest = new BulkRequest();

        final Set<String> targetIndices = new HashSet<>();
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
                final IndexRequest indexRequest = new IndexRequest().source(data);

                final String indexName = index.path("indexName").asText(null);
                if (indexName == null) {
                    throw new IllegalArgumentException("Missing indexName in " + index);
                }

                targetIndices.add(indexName);

                indexRequest.index(indexName);

                if (index.hasNonNull("indexId")) {
                    indexRequest.id(index.path("indexId").asText());
                }

                bulkRequest.add(indexRequest);
            }
        }

        for (String indexName : targetIndices) {
            if (!indexExists(indexName)) {
                createIndex(indexName);
            }
        }

        final BulkResponse result = client.execute((c, requestOptions) -> c.bulk(bulkRequest, requestOptions),
                "Unable to import fixtures.");
        if (result.hasFailures()) {
            throw new IllegalStateException("Error while bulk indexing documents: " + result.buildFailureMessage());
        }
    }

    private void createIndex(String indexName) {
        final CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName)
                .waitForActiveShards(ActiveShardCount.ONE);
        client.execute((c, requestOptions) -> c.indices().create(createIndexRequest, requestOptions));
    }

    private boolean indexExists(String indexName) {
        final GetIndexRequest indexExistsRequest = new GetIndexRequest(indexName);
        return client.execute((c, requestOptions) -> c.indices().exists(indexExistsRequest, requestOptions));
    }
}
