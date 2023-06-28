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
package org.graylog.plugins.views;

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.plugin.PluginConfigBean;
import org.joda.time.Duration;

public class ViewsConfig implements PluginConfigBean {
    private static final Duration DEFAULT_MAXIMUM_AGE_FOR_SEARCHES = Duration.standardDays(4);
    private static final String PREFIX = "views_";
    private static final String MAX_SEARCH_AGE = PREFIX + "maximum_search_age";
    public static final String AGGREGATIONS_SKIP_EMPTY_VALUES = "aggregations_skip_empty_values";

    @Parameter(MAX_SEARCH_AGE)
    private Duration maxSearchAge = DEFAULT_MAXIMUM_AGE_FOR_SEARCHES;

    @Parameter(AGGREGATIONS_SKIP_EMPTY_VALUES)
    private boolean aggregationsSkipEmptyValues = false;
}
