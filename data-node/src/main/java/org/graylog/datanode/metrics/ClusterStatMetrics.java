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
    DOC_COUNT("long", new RollupAction.IsmRollup.AvgMetric(), "$.primaries.docs.count"),
    SEARCH_LATENCY("integer", new RollupAction.IsmRollup.AvgMetric(), "$.total.search.query_time_in_millis"),
    INDEX_LATENCY("integer", new RollupAction.IsmRollup.AvgMetric(), "$.total.indexing.index_time_in_millis"),
    ;
    private final String mappingType;
    private final RollupAction.IsmRollup.AggregationMetric aggregationMetric;
    private final String clusterStat;

    ClusterStatMetrics(String mappingType, RollupAction.IsmRollup.AggregationMetric aggregationMetric, String clusterStat) {
        this.mappingType = mappingType;
        this.aggregationMetric = aggregationMetric;
        this.clusterStat = clusterStat;
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

    public String getClusterStat() {
        return clusterStat;
    }

}
