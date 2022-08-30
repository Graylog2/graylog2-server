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
package org.graylog2.indexer.fieldtypes.streamfiltered.config;

public interface Config {

    int MAX_FIELDS_TO_FILTER_AD_HOC = 50;

    int MAX_AGGREGATIONS_PER_REQUEST = 100;
    int MAX_SEARCHES_PER_MULTI_SEARCH = 100;

    int MAX_STORED_FIELDS_AGE_IN_MINUTES = 20;
}
