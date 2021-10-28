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
package org.graylog.metrics.prometheus;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PrometheusMetricFilter implements MetricFilter {
    private final List<Pattern> patterns;

    public PrometheusMetricFilter(List<MapperConfig> mapperConfigs) {
        this.patterns = mapperConfigs.stream()
                .map(mc -> globToRegex(mc.getMatch()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean matches(String name, Metric metric) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(name).matches());
    }

    /**
     * Converts the given glob pattern (see {@link MapperConfig}) to a regexp {@link Pattern}.
     *
     * @param glob the glob pattern
     * @return regexp pattern
     */
    private Pattern globToRegex(String glob) {
        final String[] parts = glob.split(Pattern.quote("*"), -1);
        final StringBuilder escapedPattern = new StringBuilder(Pattern.quote(parts[0]));

        for (int i = 1; i < parts.length; i++) {
            escapedPattern.append("([^.]*)").append(Pattern.quote(parts[i]));
        }

        return Pattern.compile("^" + escapedPattern + "$");
    }
}
