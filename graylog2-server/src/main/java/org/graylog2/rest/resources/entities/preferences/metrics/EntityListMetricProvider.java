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
package org.graylog2.rest.resources.entities.preferences.metrics;

import org.apache.shiro.subject.Subject;
import org.bson.conversions.Bson;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.resources.entities.preferences.model.MetricValue;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;

public interface EntityListMetricProvider {
    
    MetricValue compute(TimeRange timeRange, Subject subject);

    default Bson timeRangeFilter(final TimeRange timeRange, final String timestampField) {
        return and(gte(timestampField, timeRange.getFrom()),
                lte(timestampField, timeRange.getTo()));
    }

}
