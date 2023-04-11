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
package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class IndexSetPaginationResponse {
    @JsonProperty("index_sets")
    public abstract List<IndexSetSummary> indexSets();

    @JsonProperty("stats")
    public abstract Map<String, IndexSetStats> stats();

    @JsonProperty("next_cursor")
    public abstract String nextCursor();

    @JsonCreator
    public static IndexSetPaginationResponse create(@JsonProperty("index_sets") List<IndexSetSummary> indexSets,
                                                    @JsonProperty("stats") Map<String, IndexSetStats> stats,
                                                    @JsonProperty("next_cursor") String nextCursor) {
        return new AutoValue_IndexSetPaginationResponse(indexSets, stats, nextCursor);
    }
}
