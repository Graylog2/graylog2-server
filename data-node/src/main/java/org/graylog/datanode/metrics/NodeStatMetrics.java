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

import org.graylog2.indexer.datastream.policy.actions.RollupAction;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public enum NodeStatMetrics {
    CPU_LOAD("float", new RollupAction.IsmRollup.AvgMetric(), "$.os.cpu.load_average.1m"),
    MEM_FREE("float", new RollupAction.IsmRollup.AvgMetric(), "$.os.mem.free_in_bytes", NodeStatMetrics::bytesToMb),
    MEM_TOTAL_USED("integer", new RollupAction.IsmRollup.AvgMetric(), "$.os.mem.used_percent"),
    MEM_HEAP_USED("integer", new RollupAction.IsmRollup.AvgMetric(), "$.jvm.mem.heap_used_percent"),
    DISK_FREE("float", new RollupAction.IsmRollup.AvgMetric(), "$.fs.total.available_in_bytes", NodeStatMetrics::bytesToGb),
    THREAD_POOL_WRITE_THREADS("integer", new RollupAction.IsmRollup.AvgMetric(), "$.thread_pool.write.threads"),
    THREAD_POOL_WRITE_QUEUE("integer", new RollupAction.IsmRollup.AvgMetric(), "$.thread_pool.write.queue"),
    THREAD_POOL_WRITE_REJECTED("integer", new RollupAction.IsmRollup.AvgMetric(), "$.thread_pool.write.rejected"),
    THREAD_POOL_SEARCH_THREADS("integer", new RollupAction.IsmRollup.AvgMetric(), "$.thread_pool.search.threads"),
    THREAD_POOL_SEARCH_QUEUE("integer", new RollupAction.IsmRollup.AvgMetric(), "$.thread_pool.search.queue"),
    THREAD_POOL_SEARCH_REJECTED("integer", new RollupAction.IsmRollup.AvgMetric(), "$.thread_pool.search.rejected"),
    THREAD_POOL_MERGE_THREADS("integer", new RollupAction.IsmRollup.AvgMetric(), "$.thread_pool.force_merge.threads"),
    THREAD_POOL_MERGE_QUEUE("integer", new RollupAction.IsmRollup.AvgMetric(), "$.thread_pool.force_merge.queue"),
    THREAD_POOL_MERGE_REJECTED("integer", new RollupAction.IsmRollup.AvgMetric(), "$.thread_pool.force_merge.rejected"),
    ;

    private static float bytesToMb(Object v) {
        var number = (v instanceof Long) ? (long) v : (int) v;
        return number / (float) (1024 * 1024);
    }

    private static float bytesToGb(Object v) {
        var number = (v instanceof Long) ? (long) v : (int) v;
        return number / (float) (1024 * 1024 * 1024);
    }

    private final String mappingType;
    private final RollupAction.IsmRollup.AggregationMetric aggregationMetric;
    private final String nodeStat;
    private final Function<Object, Object> mappingFunction;

    NodeStatMetrics(String mappingType, RollupAction.IsmRollup.AggregationMetric aggregationMetric, String nodeStat) {
        this(mappingType, aggregationMetric, nodeStat, null);
    }

    NodeStatMetrics(String mappingType, RollupAction.IsmRollup.AggregationMetric aggregationMetric, String nodeStat, Function<Object, Object> mappingFunction) {
        this.mappingType = mappingType;
        this.aggregationMetric = aggregationMetric;
        this.nodeStat = nodeStat;
        this.mappingFunction = mappingFunction;
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

    public String getNodeStat() {
        return nodeStat;
    }

    public Object mapValue(Object value) {
        if (Objects.isNull(mappingFunction)) {
            return value;
        }
        return mappingFunction.apply(value);
    }

}
