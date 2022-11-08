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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public record SearchRequestSpec(@JsonProperty("query") String queryString,
                                @JsonProperty("streams") List<String> streams,
                                @JsonProperty("timerange") TimeRange timerange,
                                @JsonProperty("aggregation") @Valid @NotNull AggregationSpec aggregationSpec) {

    public static final RelativeRange DEFAULT_TIMERANGE = RelativeRange.create(24 * 60 * 60);

    public SearchRequestSpec {
        if (Strings.isNullOrEmpty(queryString)) {
            queryString = "*";
        }
        if (timerange == null) {
            timerange = DEFAULT_TIMERANGE;
        }
    }
}
