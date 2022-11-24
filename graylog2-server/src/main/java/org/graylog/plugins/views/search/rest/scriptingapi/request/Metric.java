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
import com.github.rvesse.airline.annotations.restrictions.NotBlank;
import org.apache.commons.lang.StringUtils;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import javax.validation.Valid;

//TODO: one or two metrics seem to have additional fields (i.e. percentile)
public record Metric(@JsonProperty("field") @Valid String fieldName,
                     @JsonProperty("function") @Valid @NotBlank String functionName,
                     @JsonProperty("sort") SortSpec.Direction sort) implements Sortable {

    @Override
    public String sortColumnName() {
        return functionName() + "(" + (fieldName() != null ? fieldName() : "") + ")";
    }

    /**
     * Creates a new Metric from its string representation
     *
     * @param metricString String representation in the form of "function:field", i.e. "avg:took_ms" or "latest:source" (you can ommit field for count function : "count" or "count:")
     * @return new Metric, or null if metricString input string is blank
     */
    public static Metric fromStringRepresentation(final String metricString) {
        if (StringUtils.isBlank(metricString)) {
            return null;
        }
        final String[] split = metricString.split(":");
        final String functionName = split[0];
        final String fieldName = split.length > 1 ? split[1] : null;
        return new Metric(fieldName, functionName, null);
    }
}
