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
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.testenv.TestableIndex;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;
import java.util.Set;

class ClusterStateApiIT {

    @Rule
    public final OpenSearchInstance opensearch = OpenSearchInstance.create();
    private ClusterStateApi clusterStateApi;

    @Test
    void testFields() {
        opensearch.withTestableEnvironment(testEnvironment -> {

            final TestableIndex firstIndex = testEnvironment.createIndex().indexDocuments(10, i1 -> Map.of(
                    "name", "doc" + i1,
                    "count", i1,
                    "created",new Date()));

            final TestableIndex secondIndex = testEnvironment.createIndex().indexDocuments(10, i -> Map.of(
                    "value", i,
                    "created",new Date()));

            // this one is empty, has no messages ingested
            final TestableIndex thirdIndex = testEnvironment.createIndex();

            testEnvironment.refreshNode();

            clusterStateApi = new ClusterStateApi(opensearch.getOfficialOpensearchClient());

            final Map<String, Set<String>> fields = clusterStateApi.fields(Set.of(firstIndex.getIndexName(), secondIndex.getIndexName(), thirdIndex.getIndexName()));
            Assertions.assertThat(fields).containsEntry(firstIndex.getIndexName(), Set.of("name", "count", "created"));
            Assertions.assertThat(fields).containsEntry(secondIndex.getIndexName(), Set.of("value", "created"));

            Assertions.assertThat(clusterStateApi.fields(Set.of(thirdIndex.getIndexName()))).isEmpty();
        });
    }
}
