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
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import { defaultOnError } from 'util/conditional/onError';
import type { Attribute, PaginatedResponseType, SearchParams } from 'stores/PaginationTypes';
import type { Metric, NodeMetric } from 'stores/metrics/MetricsStore';

export const GRAYLOG_NODE_METRIC_NAMES = {
  journalSize: 'org.graylog2.journal.size',
  journalMaxSize: 'org.graylog2.journal.size-limit',
  journalSizeRatio: 'org.graylog2.journal.utilization-ratio',
  jvmMemoryHeapUsed: 'jvm.memory.heap.used',
  jvmMemoryHeapMax: 'jvm.memory.heap.max',
  dataLakeJournalSize: 'org.graylog.plugins.datalake.output.journal.size',
  bufferInputUsage: 'org.graylog2.buffers.input.usage',
  bufferOutputUsage: 'org.graylog2.buffers.output.usage',
  bufferProcessUsage: 'org.graylog2.buffers.process.usage',
  bufferInputSize: 'org.graylog2.buffers.input.size',
  bufferOutputSize: 'org.graylog2.buffers.output.size',
  bufferProcessSize: 'org.graylog2.buffers.process.size',
  throughputIn: 'org.graylog2.throughput.input.1-sec-rate',
  throughputOut: 'org.graylog2.throughput.output.1-sec-rate',
  cpuPercent: 'org.graylog2.system.cpu.percent',
} as const;

export type GraylogNode = {
  _id?: string;
  id: string;
  node_id: string;
  short_node_id: string;
  transport_address: string;
  hostname?: string;
  last_seen?: string;
  is_leader: boolean;
  is_processing: boolean;
  lb_status?: string;
  lifecycle?: string;
  cluster_id?: string;
  codename?: string;
  facility?: string;
  started_at?: string;
  timezone?: string;
  version?: string;
  operating_system?: string;
  type?: string;
};

export type GraylogNodes = Array<GraylogNode>;
export type ClusterGraylogNode = GraylogNode & { metrics: GraylogNodeMetrics };

export type GraylogNodesResponse = {
  list: GraylogNodes;
  pagination: PaginatedResponseType;
  attributes: Array<Attribute>;
};

export const DEFAULT_GRAYLOG_NODES_SEARCH_PARAMS: SearchParams = {
  query: '',
  page: 1,
  pageSize: 0,
  sort: undefined,
};

export const fetchGraylogNodes = async (
  params: SearchParams = DEFAULT_GRAYLOG_NODES_SEARCH_PARAMS,
): Promise<GraylogNodesResponse> => {
  const url = PaginationURL('/system/cluster/nodes/paginated', params.page, params.pageSize, params.query, {
    sort: params.sort?.attributeId,
    order: params.sort?.direction,
  });

  return fetch('GET', qualifyUrl(url)).then(
    ({
      attributes,
      pagination,
      elements,
    }: {
      attributes: Array<Attribute>;
      pagination: PaginatedResponseType;
      elements: GraylogNodes;
    }) => ({
      attributes,
      list: elements,
      pagination,
    }),
  );
};

export const clusterGraylogNodesKeyFn = (searchParams: SearchParams = DEFAULT_GRAYLOG_NODES_SEARCH_PARAMS) => [
  'graylogNodes',
  searchParams,
];

const METRIC_NAMES_LIST = Object.values(GRAYLOG_NODE_METRIC_NAMES);
const METRIC_SHORT_NAMES = Object.keys(GRAYLOG_NODE_METRIC_NAMES);

export type GraylogNodeMetrics = { [key: string]: number | undefined | null };

export const EMPTY_GRAYLOG_NODE_METRICS: GraylogNodeMetrics = Object.fromEntries(
  METRIC_SHORT_NAMES.map((name) => [name, undefined]),
) as GraylogNodeMetrics;

type MetricsSummaryResponse = {
  metrics?: Array<Metric>;
};

const toNodeMetric = (response?: MetricsSummaryResponse): NodeMetric | undefined => {
  if (!response?.metrics?.length) {
    return undefined;
  }

  return Object.fromEntries(response.metrics.map((metric) => [metric.full_name, metric])) as NodeMetric;
};

const fetchGraylogNodeMetrics = async (nodeIds: Array<string>) => {
  if (!nodeIds.length) {
    return {};
  }

  const results = await Promise.all(
    nodeIds.map(async (nodeId) => {
      const url = qualifyUrl(ApiRoutes.ClusterMetricsApiController.multiple(nodeId).url);

      try {
        const response = await defaultOnError(
          fetch('POST', url, { metrics: METRIC_NAMES_LIST }),
          'Loading Graylog node metrics failed with status',
          'Could not load Graylog node metrics.',
        );

        return { nodeId, response };
      } catch (_error) {
        return { nodeId, response: undefined };
      }
    }),
  );

  return Object.fromEntries(
    results.map(({ nodeId, response }) => {
      const nodeMetric = toNodeMetric(response as MetricsSummaryResponse);
      const extracted = nodeMetric
        ? MetricsExtractor.getValuesForNode(nodeMetric, GRAYLOG_NODE_METRIC_NAMES)
        : undefined;

      return [
        nodeId,
        extracted
          ? { ...EMPTY_GRAYLOG_NODE_METRICS, ...(extracted as GraylogNodeMetrics) }
          : EMPTY_GRAYLOG_NODE_METRICS,
      ];
    }),
  );
};

export const fetchClusterGraylogNodesWithMetrics = async (
  searchParams: SearchParams = DEFAULT_GRAYLOG_NODES_SEARCH_PARAMS,
): Promise<GraylogNodesResponse & { list: Array<ClusterGraylogNode> }> => {
  const base = await fetchGraylogNodes(searchParams);
  const nodeIds = Array.from(new Set(base.list.map(({ node_id }) => node_id).filter(Boolean)));
  const metricsByNodeId = nodeIds.length ? await fetchGraylogNodeMetrics(nodeIds) : {};

  return {
    ...base,
    list: base.list.map((node) => ({
      ...node,
      metrics: metricsByNodeId[node.node_id] ?? EMPTY_GRAYLOG_NODE_METRICS,
    })),
  };
};
