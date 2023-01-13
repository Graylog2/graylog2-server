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
package org.graylog2.shared.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import java.util.Map;
import java.util.stream.Collectors;

public class CustomMemoryUsageGaugeSet implements MetricSet {
    @Override
    public Map<String, Metric> getMetrics() {
        return new MemoryUsageGaugeSet().getMetrics().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().replace("'", ""), Map.Entry::getValue));
    }
}
