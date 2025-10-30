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
  journalAppend1SecRate: 'org.graylog2.journal.append.1-sec-rate',
  journalRead1SecRate: 'org.graylog2.journal.read.1-sec-rate',
  journalSegments: 'org.graylog2.journal.segments',
  journalEntriesUncommitted: 'org.graylog2.journal.entries-uncommitted',
  journalSize: 'org.graylog2.journal.size',
  journalMaxSize: 'org.graylog2.journal.size-limit',
  journalSizeRatio: 'org.graylog2.journal.utilization-ratio',
  jvmMemoryHeapUsed: 'jvm.memory.heap.used',
  jvmMemoryHeapCommitted: 'jvm.memory.heap.committed',
  jvmMemoryHeapMaxMemory: 'jvm.memory.heap.max',
} as const;

export type GraylogNodeMetrics = { [key: string]: number | undefined | null };

const METRIC_NAMES_LIST = Object.values(METRIC_NAMES);
const METRIC_SHORT_NAMES = Object.keys(METRIC_NAMES);

const useAddMetricsToGraylogNodes = <Node extends { node_id?: string; id?: string }>(
  nodes: ReadonlyArray<Node>,
): Array<Node & { metrics: GraylogNodeMetrics }> => {
  const { metrics } = useStore(MetricsStore);
  const nodeIdentifiers = useMemo(
    () =>
      Array.from(
        new Set(
          nodes
            .map((node) => node.node_id ?? node.id)
            .filter((identifier): identifier is string => Boolean(identifier)),
        ),
      ),
    [nodes],
  );

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
        ? Object.fromEntries(METRIC_SHORT_NAMES.map((name) => [name, undefined])) as GraylogNodeMetrics
        : MetricsExtractor.getValuesForNode(nodeMetrics, METRIC_NAMES);

      return { ...acc, [nodeId]: entry };
    }, {});
  }, [metrics, nodeIdentifiers]);

  return useMemo(() => {
    const nodesWithMetrics = nodes.map((node) => {
      const identifier = node.node_id ?? node.id;
      const nodeMetrics = identifier ? metricsByNodeId[identifier] : undefined;

      return {
        ...node,
        metrics: nodeMetrics ?? {},
      };
    });

    return nodesWithMetrics;
  }, [metricsByNodeId, nodes]);
};

export default useAddMetricsToGraylogNodes;
