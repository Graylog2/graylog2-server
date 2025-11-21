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
import { useEffect, useMemo } from 'react';

import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { useStore } from 'stores/connect';

const METRIC_NAMES = {
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
} as const;

export type GraylogNodeMetrics = { [key: string]: number | undefined | null };

const METRIC_NAMES_LIST = Object.values(METRIC_NAMES);
const METRIC_SHORT_NAMES = Object.keys(METRIC_NAMES);
const EMPTY_METRICS: GraylogNodeMetrics = METRIC_SHORT_NAMES.reduce(
  (acc, name) => ({ ...acc, [name]: undefined }),
  {} as GraylogNodeMetrics,
);

const useAddMetricsToGraylogNodes = <Node extends { node_id: string }>(
  nodes: ReadonlyArray<Node>,
): Array<Node & { metrics: GraylogNodeMetrics }> => {
  const { metrics } = useStore(MetricsStore);
  const nodeIdentifiers = useMemo(() => Array.from(new Set(nodes.map((node) => node.node_id))), [nodes]);

  useEffect(() => {
    if (!nodeIdentifiers.length) {
      return undefined;
    }

    nodeIdentifiers.forEach((nodeId) => METRIC_NAMES_LIST.forEach((metric) => MetricsActions.add(nodeId, metric)));

    return () => {
      nodeIdentifiers.forEach((nodeId) => METRIC_NAMES_LIST.forEach((metric) => MetricsActions.remove(nodeId, metric)));
    };
  }, [nodeIdentifiers]);

  const metricsByNodeId = useMemo(() => {
    if (!nodeIdentifiers.length) {
      return {};
    }

    return nodeIdentifiers.reduce<Record<string, GraylogNodeMetrics>>((acc, nodeId) => {
      const nodeMetrics = metrics?.[nodeId];

      const entry: GraylogNodeMetrics = !nodeMetrics
        ? { ...EMPTY_METRICS }
        : MetricsExtractor.getValuesForNode(nodeMetrics, METRIC_NAMES);

      return { ...acc, [nodeId]: entry };
    }, {});
  }, [metrics, nodeIdentifiers]);

  return useMemo(
    () =>
      nodes.map((node) => ({
        ...node,
        metrics: metricsByNodeId[node.node_id] ?? {},
      })),
    [metricsByNodeId, nodes],
  );
};

export default useAddMetricsToGraylogNodes;
