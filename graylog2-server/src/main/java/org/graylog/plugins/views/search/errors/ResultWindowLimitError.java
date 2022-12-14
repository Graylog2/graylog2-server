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
package org.graylog.plugins.views.search.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.views.search.Query;

import javax.annotation.Nonnull;
import java.util.Locale;

public class ResultWindowLimitError extends SearchTypeError {

    private final int resultWindowLimit;

    private static final String ERROR_DESCRIPTION =
            "Result window is too large, [from + size] must be less than or equal to: [%d]. This limit can be set by changing the [index.max_result_window] index level setting, but it is recommended to use more advanced methods in order to get distant chunks of results (i.e. scroll or search after)";

    ResultWindowLimitError(@Nonnull Query query, @Nonnull String searchTypeId, int resultWindowLimit) {
        super(query, searchTypeId, String.format(Locale.US, ERROR_DESCRIPTION, resultWindowLimit));

        this.resultWindowLimit = resultWindowLimit;
    }

    @JsonProperty("result_window_limit")
    public Integer getResultWindowLimit() {
        return resultWindowLimit;
    }
}
