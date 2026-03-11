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
package org.graylog.storage.opensearch3.client;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.storage.opensearch3.testing.OpenSearchTestServerExtension;
import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.TrackHits;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ExtendWith(OpenSearchTestServerExtension.class)
class CustomOpenSearchClientIT {

    private OfficialOpensearchClient client;
    private List<String> indices;

    @BeforeEach
    void setUp(OpenSearchInstance openSearchInstance) {
        final BulkIndexRequest bulk = new BulkIndexRequest();

        indices = createLongNamedIndices(17, 255)
                .peek(index -> openSearchInstance.client().createIndex(index))
                .peek(index -> bulk.addRequest(index, Map.of("in_index", index)))
                .collect(Collectors.toList());

        openSearchInstance.client().bulkIndex(bulk);
        openSearchInstance.client().refreshNode();
        client = openSearchInstance.getOfficialOpensearchClient();
    }

    @Test
    void testUrlLimitSearchSync() {
        final double kb = indexQueryPartLength(indices);

        // any URL longer than 4kb will be rejected by default in opensearch
        Assertions.assertThat(kb).isGreaterThanOrEqualTo(4.00);

        final SearchResponse<Map> response = client.sync(c -> c.search(req -> req.index(indices).size(10).trackTotalHits(TrackHits.builder().enabled(true).build()), Map.class), "Failed to trigger search");
        Assertions.assertThat(response.hits().total().value()).isEqualTo(indices.size());
    }

    @Test
    void testUrlLimitSearchAsync() {
        final CompletableFuture<SearchResponse<Map>> response = client.async(c -> c.search(req -> req.index(indices).size(10).trackTotalHits(TrackHits.builder().enabled(true).build()), Map.class), "Failed to trigger search");

        Assertions.assertThat(response)
                .succeedsWithin(Duration.ofSeconds(10))
                .satisfies(res -> Assertions.assertThat(res.hits().total().value()).isEqualTo(indices.size()));
    }

    private static double indexQueryPartLength(List<String> indices) {
        int bytes = String.join(",", indices).getBytes(StandardCharsets.UTF_8).length;
        final double kb = bytes / 1024.0;
        return kb;
    }

    @Nonnull
    private static Stream<String> createLongNamedIndices(int howMany, int howLong) {
        final RandomStringUtils randomStringUtils = RandomStringUtils.secure();
        return IntStream.range(0, howMany).mapToObj(i -> randomStringUtils.nextAlphanumeric(howLong).toLowerCase(Locale.ROOT));
    }
}
