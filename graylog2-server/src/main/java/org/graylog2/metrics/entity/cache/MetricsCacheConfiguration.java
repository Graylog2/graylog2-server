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
package org.graylog2.metrics.entity.cache;

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

    /** Query time range for long-window metrics (message count, associated entities). */
    public static final int RANGE_SECONDS_24H = 86400;
    /** Query time range for short-window metrics (avg/max processing time, failure counts). */
    public static final int RANGE_SECONDS_15M = 900;
    /**
     * Upper bound for the {@code size} parameter in OpenSearch terms aggregations.
     * Large values increase memory and compute on every shard ({@code shard_size = size * 1.5 + 10}).
     * 10,000 is well beyond any realistic cardinality (streams per input, inputs per stream)
     * while staying safely below the default {@code search.max_buckets} limit (65,535).
     *
     * @see <a href="https://docs.opensearch.org/latest/aggregations/bucket/terms/">OpenSearch terms aggregation</a>
     */
    public static final int MAX_TERMS_SIZE = 10_000;

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
