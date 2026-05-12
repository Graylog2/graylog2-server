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
package org.graylog2.metrics.cache;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import org.graylog2.configuration.converters.JavaDurationConverter;

import java.time.Duration;

@DocumentationSection(heading = "Entity metrics cache configuration", description = "Configuration for the metrics_cache MongoDB collection used to cache expensive entity metrics (e.g. OpenSearch aggregations).")
public class MetricsCacheConfiguration {

    public static final String METRICS_CACHE_TTL_SHORT = "metrics_cache_ttl_short";
    public static final String METRICS_CACHE_TTL_LONG = "metrics_cache_ttl_long";
    public static final String METRICS_CACHE_CLEANUP_TTL = "metrics_cache_cleanup_ttl";

    @Documentation(value = "Cache TTL for short-window metrics (e.g. avg/max processing time with a 15m window).")
    @Parameter(value = "metrics_cache_ttl_short", converter = JavaDurationConverter.class)
    private Duration shortTtl = Duration.ofMinutes(1);

    @Documentation(value = "Cache TTL for long-window metrics (e.g. message count, associated entities with a 24h window).")
    @Parameter(value = "metrics_cache_ttl_long", converter = JavaDurationConverter.class)
    private Duration longTtl = Duration.ofMinutes(5);

    @Documentation(value = "TTL for automatic cleanup of stale cache entries (e.g. deleted entities). Applied via MongoDB TTL index.")
    @Parameter(value = "metrics_cache_cleanup_ttl", converter = JavaDurationConverter.class)
    private Duration cleanupTtl = Duration.ofHours(24);

    public Duration getShortTtl() {
        return shortTtl;
    }

    public Duration getLongTtl() {
        return longTtl;
    }

    public Duration getCleanupTtl() {
        return cleanupTtl;
    }
}
