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

import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.ShardFailure;
import org.opensearch.client.opensearch._types.ShardStatistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomOpenSearchClientTest {

    @Test
    void throwOnShardFailuresDoesNothingWhenAllShardsSucceeded() {
        final ShardStatistics shards = ShardStatistics.of(b -> b.total(5).successful(5).failed(0));

        assertThatCode(() -> CustomOpenSearchClient.throwOnShardFailures(shards)).doesNotThrowAnyException();
    }

    @Test
    void throwOnShardFailuresThrowsWhenAnyShardFailed() {
        final ShardFailure failure = ShardFailure.of(b -> b
                .index("graylog_0")
                .shard(2)
                .reason(ErrorCause.of(c -> c.type("query_shard_exception").reason("parse error")))
        );
        final ShardStatistics shards = ShardStatistics.of(b -> b
                .total(5)
                .successful(4)
                .failed(1)
                .failures(failure)
        );

        assertThatThrownBy(() -> CustomOpenSearchClient.throwOnShardFailures(shards))
                .isInstanceOf(OpenSearchException.class)
                .satisfies(thrown -> {
                    final var ex = (OpenSearchException) thrown;
                    assertThat(ex.status()).isEqualTo(500);
                    assertThat(ex.error().type()).isEqualTo("shard_failure");
                    assertThat(ex.error().reason()).isEqualTo("1 of 5 shards failed");
                    assertThat(ex.error().rootCause()).singleElement().satisfies(rc -> {
                        assertThat(rc.type()).isEqualTo("query_shard_exception");
                        assertThat(rc.reason()).isEqualTo("parse error");
                    });
                    assertThat(ex.error().causedBy()).satisfies(cb -> {
                        assertThat(cb.type()).isEqualTo("query_shard_exception");
                        assertThat(cb.reason()).isEqualTo("parse error");
                    });
                });
    }
}
