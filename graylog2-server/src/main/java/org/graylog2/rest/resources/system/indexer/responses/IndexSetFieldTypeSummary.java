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

import java.util.List;

public record IndexSetFieldTypeSummary(@JsonProperty(INDEX_SET_ID) String id,
                                       @JsonProperty(INDEX_SET_TITLE) String title,
                                       @JsonProperty(STREAM_TITLES) List<String> streams,
                                       @JsonProperty(FIELD_TYPE_HISTORY) List<String> fieldTypeHistory
) {
    public static final String INDEX_SET_ID = "index_set_id";
    public static final String INDEX_SET_TITLE = "index_set_title";
    public static final String STREAM_TITLES = "stream_titles";
    public static final String FIELD_TYPE_HISTORY = "types";
}
