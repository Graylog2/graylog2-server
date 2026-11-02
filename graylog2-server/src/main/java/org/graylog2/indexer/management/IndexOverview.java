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
package org.graylog2.indexer.management;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import javax.annotation.Nullable;

/**
 * An index as the Index Management tab shows it: OpenSearch's view plus the Graylog index set it belongs to.
 */
public record IndexOverview(@JsonProperty("index") String index,
                            @JsonProperty("health") @Nullable String health,
                            @JsonProperty("status") @Nullable String status,
                            @JsonProperty("primary_shards") @Nullable Integer primaryShards,
                            @JsonProperty("replicas") @Nullable Integer replicas,
                            @JsonProperty("docs_count") @Nullable Long docsCount,
                            @JsonProperty("store_size_bytes") @Nullable Long storeSizeBytes,
                            @JsonProperty("index_set_id") @Nullable String indexSetId,
                            @JsonProperty("index_set_title") @Nullable String indexSetTitle,
                            @JsonProperty("is_write_index") boolean isWriteIndex,
                            @JsonProperty("tier") @Nullable String tier) {

    /** Hot: a regular index. Warm: a searchable snapshot (Graylog's warm tier). Room for "archive"/"cold" later. */
    public static final String TIER_HOT = "hot";
    public static final String TIER_WARM = "warm";

    public record Response(@JsonProperty("indices") List<IndexOverview> indices) {
    }
}
