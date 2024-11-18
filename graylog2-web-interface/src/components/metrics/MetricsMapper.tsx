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
import { useEffect } from 'react';

import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { useStore } from 'stores/connect';

type Props = {
  map: {},
  computeValue: (map: any) => string,
}

const MetricsMapper = ({ map, computeValue }: Props) => {
  const { metrics } = useStore(MetricsStore);

  useEffect(() => {
    Object.keys(map).forEach((name) => MetricsActions.addGlobal(map[name]));

    return () => {
      Object.keys(map).forEach((name) => MetricsActions.removeGlobal(map[name]));
    };
  });

  if (!metrics) {
    return null;
  }

  const metricsMap = {};

  Object.keys(metrics).forEach((nodeId) => {
    Object.keys(map).forEach((key) => {
      const metricName = map[key];

      if (metrics[nodeId][metricName]) {
        // Only create the node entry if we actually have data
        if (!metricsMap[nodeId]) {
          metricsMap[nodeId] = {};
        }

        metricsMap[nodeId][key] = metrics[nodeId][metricName];
      }
    });
  });

  const value = computeValue(metricsMap);

  return (
    <span>
      {value}
    </span>
  );
};

export default MetricsMapper;
