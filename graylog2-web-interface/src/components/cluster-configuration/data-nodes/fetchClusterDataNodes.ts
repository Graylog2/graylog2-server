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
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { defaultOnError } from 'util/conditional/onError';
import type { Metric, NodeMetric } from 'stores/metrics/MetricsStore';
import type { SearchParams } from 'stores/PaginationTypes';
import type { DataNode } from 'components/datanode/Types';
import { fetchDataNodes, keyFn as dataNodesKeyFn, type DataNodeResponse } from 'components/datanode/hooks/useDataNodes';

export const DATANODE_METRIC_NAMES = {
  totalMemory: 'opensearch.os.mem.total_in_bytes',
  usedMemory: 'opensearch.os.mem.used_in_bytes',
  jvmMemoryHeapUsed: 'opensearch.jvm.mem.heap_used_in_bytes',
  jvmMemoryHeapMax: 'opensearch.jvm.mem.heap_max_in_bytes',
  jvmMemoryHeapUsedPercent: 'opensearch.jvm.mem.heap_used_percent',
  cpuLoadAverage1m: 'opensearch.os.cpu.load_average.1m',
  cpuPercent: 'opensearch.os.cpu.percent',
  indexTotal: 'opensearch.indices.indexing.index_total',
  indexTimeInMillis: 'opensearch.indices.indexing.index_time_in_millis',
  totalFsBytes: 'opensearch.fs.total.total_in_bytes',
  availableFsBytes: 'opensearch.fs.total.available_in_bytes',
} as const;

type MetricNameKey = keyof typeof DATANODE_METRIC_NAMES;
const METRIC_SHORT_NAMES = Object.keys(DATANODE_METRIC_NAMES) as Array<MetricNameKey>;
const METRIC_NAMES_LIST = Object.values(DATANODE_METRIC_NAMES);

export type DataNodeMetrics = Partial<Record<MetricNameKey, number | undefined | null>>;
export type ClusterDataNode = DataNode & { metrics: DataNodeMetrics };

export const DEFAULT_CLUSTER_DATA_NODES_SEARCH_PARAMS: SearchParams = {
  query: '-datanode_status:UNAVAILABLE',
  page: 1,
  pageSize: 0,
  sort: undefined,
};

type MetricsSummaryResponse = {
  total: number;
  metrics: Array<Metric>;
};

const toNodeMetric = (response?: MetricsSummaryResponse): NodeMetric | undefined => {
  if (!response?.metrics?.length) {
    return undefined;
  }

  return Object.fromEntries(response.metrics.map((metric) => [metric.full_name, metric])) as NodeMetric;
};

export const buildMetricsWithDefaults = (metrics: Partial<DataNodeMetrics> = {}): DataNodeMetrics =>
  Object.fromEntries(METRIC_SHORT_NAMES.map((key) => [key, metrics[key]])) as DataNodeMetrics;

const extractMetrics = (response?: MetricsSummaryResponse): DataNodeMetrics => {
  const nodeMetric = toNodeMetric(response);

  if (!nodeMetric) {
    return buildMetricsWithDefaults();
  }

  const extracted = MetricsExtractor.getValuesForNode(nodeMetric, DATANODE_METRIC_NAMES);

  return buildMetricsWithDefaults(extracted as DataNodeMetrics);
};

const fetchMetrics = async (hostname: string) => {
  try {
    return await defaultOnError(
      fetch('POST', qualifyUrl(`/datanodes/${hostname}/rest/metrics/multiple`), { metrics: METRIC_NAMES_LIST }),
      `Loading metrics for Data Node "${hostname}" failed`,
      'Could not load Data Node metrics.',
    );
  } catch (_error) {
    return undefined;
  }
};

export const fetchMetricsForHostnames = async (hostnames: string[]) => {
  const responses = await Promise.all(
    hostnames.map(async (hostname) => ({ hostname, response: await fetchMetrics(hostname) })),
  );

  return Object.fromEntries(responses.map(({ hostname, response }) => [hostname, extractMetrics(response)]));
};

export const fetchClusterDataNodesWithMetrics = async (
  searchParams: SearchParams = DEFAULT_CLUSTER_DATA_NODES_SEARCH_PARAMS,
): Promise<DataNodeResponse & { list: Array<ClusterDataNode> }> => {
  const base = await fetchDataNodes(searchParams);
  const compatibleHostnames = new Set<string>();

  base.list.forEach(({ hostname, version_compatible }) => {
    if (version_compatible !== false && hostname) {
      compatibleHostnames.add(hostname);
    }
  });

  const metricsByHostname = compatibleHostnames.size
    ? await fetchMetricsForHostnames([...compatibleHostnames])
    : {};

  return {
    ...base,
    list: base.list.map((node) => ({
      ...node,
      metrics: metricsByHostname[node.hostname] ?? buildMetricsWithDefaults(),
    })),
  };
};

export const clusterDataNodesKeyFn = (searchParams: SearchParams = DEFAULT_CLUSTER_DATA_NODES_SEARCH_PARAMS) =>
  dataNodesKeyFn(searchParams);
