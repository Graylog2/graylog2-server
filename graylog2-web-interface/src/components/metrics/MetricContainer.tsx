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
import * as React from 'react';
import { useMemo } from 'react';

import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import { useMetrics } from 'hooks/useMetrics';

type Props = {
  name: string;
  zeroOnMissing?: boolean;
  children: React.ReactElement[] | React.ReactElement;
};

const MetricContainer = ({ name, zeroOnMissing = true, children }: Props) => {
  const metricNames = useMemo(() => [name], [name]);
  const { data: metrics, isLoading } = useMetrics(metricNames);

  if (isLoading || Object.keys(metrics).length === 0) {
    return <span>Loading...</span>;
  }

  let throughput = Object.keys(metrics)
    .map((nodeId) => MetricsExtractor.getValuesForNode(metrics[nodeId], { throughput: name }))
    .reduce(
      (
        accumulator: { throughput?: number },
        currentMetric: { throughput: number | undefined | null },
      ): { throughput?: number } => ({ throughput: (accumulator.throughput || 0) + (currentMetric.throughput || 0) }),
      {},
    );

  if (zeroOnMissing && (!throughput || !throughput.throughput)) {
    throughput = { throughput: 0 };
  }

  return (
    <div>
      {React.Children.map(children, (child) =>
        React.cloneElement(child, { metric: { full_name: name, count: throughput.throughput } }),
      )}
    </div>
  );
};

export default MetricContainer;
