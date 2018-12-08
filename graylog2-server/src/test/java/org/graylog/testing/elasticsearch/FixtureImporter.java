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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.searchbox.client.JestClient;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import org.graylog2.jackson.TypeReferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixtureImporter {
    private static final Logger LOG = LoggerFactory.getLogger(FixtureImporter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public void importResource(URL resource, JestClient client) {
        LOG.debug("Importing fixture resource: {}", resource);
        try {
            final JsonNode root = OBJECT_MAPPER.readValue(resource, JsonNode.class);
            importNode(client, root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void importNode(JestClient client, JsonNode root) throws IOException {
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

                indexBuilder.index(index.path("indexName").asText());

                if (index.hasNonNull("indexType")) {
                    indexBuilder.type(index.path("indexType").asText());
                }

                if (index.hasNonNull("indexId")) {
                    indexBuilder.id(index.path("indexId").asText());
                }

                bulkBuilder.addAction(indexBuilder.build());
            }
        }

        final BulkResult result = client.execute(bulkBuilder.build());
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
}
