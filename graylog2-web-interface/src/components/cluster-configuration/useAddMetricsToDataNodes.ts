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
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { defaultOnError } from 'util/conditional/onError';
import type { Metric, NodeMetric } from 'stores/metrics/MetricsStore';
import type { DataNode } from 'components/datanode/Types';

const METRIC_NAMES = {
  cpuLoad: 'cpu_load',
  memFree: 'mem_free',
  memTotalUsed: 'mem_total_used',
  memHeapUsed: 'mem_heap_used',
  jvmMemoryHeapUsage: 'jvm.memory.heap.usage',
  jvmMemoryHeapUsed: 'jvm.memory.heap.used',
  jvmMemoryHeapMax: 'jvm.memory.heap.max',
  diskFree: 'disk_free',
  dnHeapUsage: 'dn_heap_usage',
  dnNonHeapUsage: 'dn_non_heap_usage',
  dnProcessors: 'dn_processors',
  dnThreadCount: 'dn_thread_count',
  dnGcTime: 'dn_gc_time',
} as const;

type MetricNameKey = keyof typeof METRIC_NAMES;
const METRIC_SHORT_NAMES = Object.keys(METRIC_NAMES) as Array<MetricNameKey>;
const METRIC_NAMES_LIST = Object.values(METRIC_NAMES);

export type DataNodeMetrics = Partial<Record<MetricNameKey, number | undefined | null>>;

type MetricsSummaryResponse = {
  total: number;
  metrics: Array<Metric>;
};

const buildMetricsWithDefaults = (metrics: Partial<DataNodeMetrics> = {}): DataNodeMetrics =>
  METRIC_SHORT_NAMES.reduce<DataNodeMetrics>((acc, key) => ({ ...acc, [key]: metrics[key] }), {} as DataNodeMetrics);

const toNodeMetric = (response?: MetricsSummaryResponse): NodeMetric | undefined => {
  if (!response?.metrics?.length) {
    return undefined;
  }

  return response.metrics.reduce<NodeMetric>((acc, metric) => ({ ...acc, [metric.full_name]: metric }), {} as NodeMetric);
};

const extractMetrics = (response?: MetricsSummaryResponse): DataNodeMetrics => {
  const nodeMetric = toNodeMetric(response);

  if (!nodeMetric) {
    return buildMetricsWithDefaults();
  }

  const extracted = MetricsExtractor.getValuesForNode(nodeMetric, METRIC_NAMES);

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

const fetchMetricsForHostnames = async (hostnames: string[]) => {
  const responses = await Promise.all(
    hostnames.map(async (hostname) => ({ hostname, response: await fetchMetrics(hostname) })),
  );

  return responses.reduce<Record<string, DataNodeMetrics>>((acc, { hostname, response }) => ({
    ...acc,
    [hostname]: extractMetrics(response),
  }), {});
};

type UseAddMetricsToDataNodesOptions = {
  refetchInterval?: number | false;
  enabled?: boolean;
};

const useAddMetricsToDataNodes = <Node extends Pick<DataNode, 'hostname'>>(
  nodes: ReadonlyArray<Node>,
  { refetchInterval = false, enabled = true }: UseAddMetricsToDataNodesOptions = {},
): Array<Node & { metrics: DataNodeMetrics }> => {
  const hostnames = useMemo(
    () => Array.from(new Set(nodes.map(({ hostname }) => hostname))).sort(),
    [nodes],
  );

  const { data: metricsByHostname = {} } = useQuery({
    queryKey: ['datanode-metrics', hostnames],
    queryFn: () => fetchMetricsForHostnames(hostnames),
    enabled: enabled && hostnames.length > 0,
    refetchInterval,
  });

  return useMemo(
    () =>
      nodes.map((node) => ({
        ...node,
        metrics: metricsByHostname[node.hostname] ?? buildMetricsWithDefaults(),
      })),
    [metricsByHostname, nodes],
  );
};

export default useAddMetricsToDataNodes;
