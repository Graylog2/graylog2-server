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

package org.graylog.storage.opensearch3.views.searchtypes.pivot;

import org.opensearch.client.opensearch._types.aggregations.Aggregation;

import java.util.ArrayList;
import java.util.List;

public class MutableNamedAggregationBuilder {

    private final String name;
    private final Aggregation.Builder.ContainerBuilder aggregation;
    private final List<MutableNamedAggregationBuilder> subAggregations;
    private Aggregation builtAggregation = null;

    public MutableNamedAggregationBuilder(String name,
                                          Aggregation.Builder.ContainerBuilder containerBuilder) {
        this.name = name;
        this.aggregation = containerBuilder;
        this.subAggregations = new ArrayList<>();
    }

    public void subAggregation(MutableNamedAggregationBuilder subAggregation) {
        if (builtAggregation != null) {
            throw new IllegalStateException("Subaggregations cannot be added to built aggregations.");
        }
        this.subAggregations.add(subAggregation);
    }

    public String getName() {
        return name;
    }

    public Aggregation build() {
        if (builtAggregation == null) {
            subAggregations.forEach(agg -> {
                aggregation.aggregations(agg.getName(), agg.build());
            });
            builtAggregation = aggregation.build();
        }
        return builtAggregation;
    }
}
