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
package org.graylog.storage.opensearch3.cluster;

import org.assertj.core.api.Assertions;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.storage.opensearch3.testing.OpenSearchTestServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

@ExtendWith(OpenSearchTestServerExtension.class)
class ClusterStateApiIT {

    private static final String MY_INDEX_NAME = "my_index_0";
    private static final String MY_OTHER_INDEX_NAME = "my_other_index_0";
    private static final String MY_EMPTY_INDEX_NAME = "my_empty_index_0";

    @Test
    void testFields(OpenSearchInstance opensearch) {

        final OfficialOpensearchClient client = opensearch.getOfficialOpensearchClient();

        generateIndex(client, MY_INDEX_NAME, 10, i -> new MyDocument("doc" + i, i, new Date()));
        generateIndex(client, MY_OTHER_INDEX_NAME, 10, i -> new MyDatapoint(i, new Date()));

        // this one is empty and has no document and no mappings
        client.sync(c -> c.indices().create(r -> r.index(MY_EMPTY_INDEX_NAME)), "Failed to create index " + MY_EMPTY_INDEX_NAME);

        opensearch.client().refreshNode();
        final ClusterStateApi clusterStateApi = new ClusterStateApi(client);

        final Map<String, Set<String>> fields = clusterStateApi.fields(Set.of(MY_INDEX_NAME, MY_OTHER_INDEX_NAME));
        Assertions.assertThat(fields).containsEntry(MY_INDEX_NAME, Set.of("name", "count", "created"));
        Assertions.assertThat(fields).containsEntry(MY_OTHER_INDEX_NAME, Set.of("value", "created"));

        Assertions.assertThat(clusterStateApi.fields(Set.of(MY_EMPTY_INDEX_NAME))).isEmpty();
    }

    private <T> void generateIndex(OfficialOpensearchClient client, String indexName, int documentsCount, Function<Integer, T> documentCreator) {
        client.sync(c -> c.indices().create(r -> r.index(indexName)), "Failed to create index");
        BulkRequest.Builder br = new BulkRequest.Builder();

        final List<BulkOperation> bulkIndexRequests = IntStream.range(0, documentsCount)
                .mapToObj(documentCreator::apply)
                .map(doc -> indexObjectOperation(doc, indexName))
                .toList();

        client.sync(c -> c.bulk(br.operations(bulkIndexRequests).build()), "Failed to index documents");
    }

    private BulkOperation indexObjectOperation(Object doc, String indexName) {
        return BulkOperation.of(bulk -> bulk.index(r -> r.index(indexName).document(doc)));
    }


    private record MyDocument(String name, int count, Date created) {}

    private record MyDatapoint(int value, Date created) {}
}
