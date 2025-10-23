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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.TimeUnitInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;

import java.util.function.Function;

public class GroupingToBucketSpecMapper implements Function<Grouping, BucketSpec> {
    /**
     * Only 'scaling' or 'timeunit' or none of both should be present, this is validated in Grouping.java on deserializing the JSON
     */
    @Override
    public BucketSpec apply(final Grouping grouping) {
        if(grouping.scaling().isPresent()) {
            return Time.builder()
                    .field(grouping.requestedField().name())
                    .type(Time.NAME)
                    .interval(AutoInterval.create(grouping.scaling().get()))
                    .build();
        } else if(grouping.timeunit().isPresent()) {
            return Time.builder()
                    .field(grouping.requestedField().name())
                    .type(Time.NAME)
                    .interval(TimeUnitInterval.Builder.builder().timeunit(grouping.timeunit().get()).build())
                    .build();
        } else {
            return Values.builder()
                        .field(grouping.requestedField().name())
                        .type(Values.NAME)
                        .limit(grouping.limit().orElse(Values.DEFAULT_LIMIT))
                        .build();
        }
    }
}
