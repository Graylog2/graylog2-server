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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.graylog2.indexer.indexset.IndexSetCategory;
import org.graylog2.indexer.indexset.IndexSetConfig;

/**
 * One row of the index sets entity table. It carries the {@link IndexSetResponse} fields plus the derived
 * values only the table needs.
 */
public record IndexSetOverviewResponse(@JsonUnwrapped IndexSetResponse indexSet,
                                       @JsonProperty("category") String category,
                                       @JsonProperty("can_have_profile") boolean canHaveProfile,
                                       @JsonProperty("stream_count") long streamCount) {

    public static IndexSetOverviewResponse create(IndexSetConfig config, boolean isDefault, long streamCount) {
        return new IndexSetOverviewResponse(
                IndexSetResponse.fromIndexSetConfig(config, isDefault, null),
                IndexSetCategory.of(config).value(),
                config.canHaveProfile(),
                streamCount);
    }
}
