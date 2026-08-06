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
package org.graylog2.system.stats.elasticsearch;

/**
 * Live per-node runtime utilization of a search-cluster node, read from {@code _nodes/stats/os,jvm}. Distinct from
 * {@link NodeOSInfo} (static OS facts) and the cluster-wide node counts in {@link NodesStats}: these are the sampled
 * utilization percentages the health reporters window.
 *
 * @param name                the node's display name (from {@code /name}).
 * @param cpuPercent          OS CPU utilization {@code 0..100}, or {@code -1} when the source did not report it.
 * @param jvmHeapUsedPercent  JVM heap used {@code 0..100} (OpenSearch reports the percentage directly), or {@code -1}.
 */
public record NodeUtilization(String name, double cpuPercent, double jvmHeapUsedPercent) {
}
