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

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;


public record AggregationSpec(@JsonProperty("group_by") @Valid @NotEmpty List<Grouping> groupings,
                              @JsonProperty("metrics") @Valid @NotEmpty List<Metric> metrics,
                              @JsonProperty("grouping_sort_has_priority") boolean groupingSortHasPriority) {

    public boolean hasCustomSort() {
        return groupings().stream().anyMatch(gr -> gr.sort() != null) || metrics().stream().anyMatch(m -> m.sort() != null);
    }
}
