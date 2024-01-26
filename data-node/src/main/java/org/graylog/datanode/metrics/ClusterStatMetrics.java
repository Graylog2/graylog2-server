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
package org.graylog.datanode.metrics;

import org.graylog.storage.opensearch2.ism.policy.actions.RollupAction;

import java.util.Locale;

public enum ClusterStatMetrics {
    DOC_COUNT("long", new RollupAction.IsmRollup.AvgMetric(), "$._all.primaries.docs.count", false),
    SHARDS_TOTAL("integer", new RollupAction.IsmRollup.AvgMetric(), "$._shards.total", false),
    SHARDS_SUCCESSFUL("integer", new RollupAction.IsmRollup.AvgMetric(), "$._shards.successful", false),
    SHARDS_FAILED("integer", new RollupAction.IsmRollup.AvgMetric(), "$._shards.failed", false),
    SEARCH_LATENCY("integer", new RollupAction.IsmRollup.AvgMetric(), "$._all.total.search.query_time_in_millis", true),
    INDEX_LATENCY("integer", new RollupAction.IsmRollup.AvgMetric(), "$._all.total.indexing.index_time_in_millis", true),
    SEARCH_OPS("long", new RollupAction.IsmRollup.AvgMetric(), "$._all.total.search.query_total", true),
    INDEX_OPS("long", new RollupAction.IsmRollup.AvgMetric(), "$._all.total.indexing.index_total", true),

    ;
    private final String mappingType;
    private final RollupAction.IsmRollup.AggregationMetric aggregationMetric;
    private final String clusterStat;
    private final boolean rateMetric;
    private static final String RATE_SUFFIX = "_rate";


    ClusterStatMetrics(String mappingType, RollupAction.IsmRollup.AggregationMetric aggregationMetric, String clusterStat, boolean rateMetric) {
        this.mappingType = mappingType;
        this.aggregationMetric = aggregationMetric;
        this.clusterStat = clusterStat;
        this.rateMetric = rateMetric;
    }

    public String getMappingType() {
        return mappingType;
    }

    public RollupAction.IsmRollup.AggregationMetric getAggregationMetric() {
        return aggregationMetric;
    }

    public String getFieldName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getRateFieldName() {
        if (!isRateMetric()) {
            throw new RuntimeException("Metric is not a rate metric");
        }
        return getFieldName() + RATE_SUFFIX;
    }

    public String getClusterStat() {
        return clusterStat;
    }

    public boolean isRateMetric() {
        return rateMetric;
    }

}
