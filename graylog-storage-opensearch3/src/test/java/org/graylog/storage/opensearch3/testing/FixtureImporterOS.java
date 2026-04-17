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
package org.graylog.storage.opensearch3.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog2.jackson.TypeReferences;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.WaitForActiveShards;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FixtureImporterOS implements FixtureImporter {
    private static final Logger LOG = LoggerFactory.getLogger(FixtureImporter.class);
    private final OfficialOpensearchClient client;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public FixtureImporterOS(OfficialOpensearchClient client) {
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
        final BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

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
                IndexOperation.Builder indexRequest = new IndexOperation.Builder();
                indexRequest.document(data);

                final String indexName = index.path("indexName").asText(null);
                if (indexName == null) {
                    throw new IllegalArgumentException("Missing indexName in " + index);
                }

                targetIndices.add(indexName);

                indexRequest.index(indexName);

                if (index.hasNonNull("indexId")) {
                    indexRequest.id(index.path("indexId").asText());
                }

                bulkRequest.operations(BulkOperation.of(b -> b.index(indexRequest.build())));
            }
        }

        for (String indexName : targetIndices) {
            if (!indexExists(indexName)) {
                createIndex(indexName);
            }
        }

        final BulkResponse result = client.sync(c -> c.bulk(bulkRequest.build()), "Unable to import fixtures.");
        if (result.errors()) {
            throw new IllegalStateException("Error while bulk indexing documents: " + result.items().stream()
                    .map(BulkResponseItem::error)
                    .filter(Objects::nonNull)
                    .map(ErrorCause::reason)
                    .collect(Collectors.joining(","))
            );
        }
    }

    private void createIndex(String indexName) {
        final CreateIndexRequest createIndexRequest = CreateIndexRequest.of(r -> r
                .index(indexName)
                .waitForActiveShards(WaitForActiveShards.of(s -> s.count(1)))
        );
        client.sync(c -> c.indices().create(createIndexRequest), "Error creating index " + indexName);
    }

    private boolean indexExists(String indexName) {
        final ExistsRequest indexExistsRequest = ExistsRequest.of(r -> r.index(indexName));
        return client.sync(c -> c.indices().exists(indexExistsRequest), "Error getting index").value();
    }
}
