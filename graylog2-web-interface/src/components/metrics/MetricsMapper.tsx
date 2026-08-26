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

import { useMetrics } from 'hooks/useMetrics';

type Props = {
  map: {};
  computeValue: (map: any) => string;
};

const MetricsMapper = ({ map, computeValue }: Props) => {
  const metricNames = useMemo(() => Object.values(map) as string[], [map]);
  const { data: metrics, isLoading } = useMetrics(metricNames);

  if (isLoading || Object.keys(metrics).length === 0) {
    return null;
  }

  const metricsMap = {};

  Object.keys(metrics).forEach((nodeId) => {
    Object.keys(map).forEach((key) => {
      const metricName = map[key];

      if (metrics[nodeId][metricName]) {
        if (!metricsMap[nodeId]) {
          metricsMap[nodeId] = {};
        }

        metricsMap[nodeId][key] = metrics[nodeId][metricName];
      }
    });
  });

  const value = computeValue(metricsMap);

  return <span>{value}</span>;
};

export default MetricsMapper;
