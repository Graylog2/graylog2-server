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
import { useContext, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import { ClusterNodeMetrics } from '@graylog/server-api';

import MetricsContext from 'contexts/MetricsContext';
import type { ClusterMetric, Metric, NodeMetric } from 'types/metrics';

const useMetricsContext = () => {
  const context = useContext(MetricsContext);

  if (!context) {
    throw new Error('useMetrics must be used within a MetricsProvider');
  }

  return context;
};

export const useMetrics = (metricNames: string[]): { data: ClusterMetric; isLoading: boolean } => {
  const { metrics, isLoading, subscribe, unsubscribe } = useMetricsContext();

  const sortedNames = useMemo(
    () => [...metricNames].sort(),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [JSON.stringify(metricNames)],
  );

  useEffect(() => {
    if (sortedNames.length === 0) return undefined;

    subscribe(sortedNames);

    return () => {
      unsubscribe(sortedNames);
    };
  }, [sortedNames, subscribe, unsubscribe]);

  return { data: metrics, isLoading };
};

export const useNodeMetrics = (
  nodeId: string,
  metricNames: string[],
): { data: NodeMetric | undefined; isLoading: boolean } => {
  const { data: metrics, isLoading } = useMetrics(metricNames);

  return { data: metrics[nodeId], isLoading };
};

export const useMetric = (metricName: string): { data: ClusterMetric; isLoading: boolean } => {
  const names = useMemo(() => [metricName], [metricName]);

  return useMetrics(names);
};

export const useNodeMetric = (
  nodeId: string,
  metricName: string,
): { data: NodeMetric | undefined; isLoading: boolean } => {
  const names = useMemo(() => [metricName], [metricName]);

  return useNodeMetrics(nodeId, names);
};

export const useMetricsNames = (
  nodeId: string | undefined,
  namespace: string = 'org',
): { data: Metric[] | undefined; isLoading: boolean } => {
  const { data, isLoading } = useQuery({
    queryKey: ['metrics', 'names', nodeId, namespace],
    queryFn: () => ClusterNodeMetrics.byNamespace(nodeId!, namespace).then(({ metrics }) => metrics as Metric[]),
    enabled: nodeId !== undefined,
  });

  return { data, isLoading };
};

const THROUGHPUT_INPUT = 'org.graylog2.throughput.input.1-sec-rate';
const THROUGHPUT_OUTPUT = 'org.graylog2.throughput.output.1-sec-rate';
const THROUGHPUT_METRICS = [THROUGHPUT_INPUT, THROUGHPUT_OUTPUT];

export const useGlobalThroughput = (): { input: number; output: number; isLoading: boolean } => {
  const { data: metrics, isLoading } = useMetrics(THROUGHPUT_METRICS);

  const input = useMemo(
    () =>
      Object.keys(metrics).reduce((sum, nodeId) => {
        const metric = metrics[nodeId]?.[THROUGHPUT_INPUT];

        return sum + (metric?.metric && 'value' in metric.metric ? metric.metric.value : 0);
      }, 0),
    [metrics],
  );

  const output = useMemo(
    () =>
      Object.keys(metrics).reduce((sum, nodeId) => {
        const metric = metrics[nodeId]?.[THROUGHPUT_OUTPUT];

        return sum + (metric?.metric && 'value' in metric.metric ? metric.metric.value : 0);
      }, 0),
    [metrics],
  );

  return { input, output, isLoading };
};
