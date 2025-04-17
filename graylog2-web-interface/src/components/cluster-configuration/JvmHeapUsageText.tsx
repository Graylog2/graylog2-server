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
import { useEffect, useMemo } from 'react';

import NumberUtils from 'util/NumberUtils';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { useStore } from 'stores/connect';

type Props = {
  nodeId: string;
};

const metricNames = {
  usedMemory: 'jvm.memory.heap.used',
  committedMemory: 'jvm.memory.heap.committed',
  maxMemory: 'jvm.memory.heap.max',
};

const JvmHeapUsageText = ({ nodeId }: Props) => {
  const { metrics } = useStore(MetricsStore);

  useEffect(() => {
    Object.keys(metricNames).forEach((metricShortName) => MetricsActions.add(nodeId, metricNames[metricShortName]));

    return () => {
      Object.keys(metricNames).forEach((metricShortName) =>
        MetricsActions.remove(nodeId, metricNames[metricShortName]),
      );
    };
  }, [nodeId]);

  const extractedMetrics = useMemo(() => {
    if (metrics?.[nodeId]) {
      const extractedMetric = MetricsExtractor.getValuesForNode(metrics[nodeId], metricNames);
      const { maxMemory, usedMemory, committedMemory } = extractedMetric;

      if (maxMemory) {
        extractedMetric.usedPercentage = maxMemory === 0 ? 0 : Math.ceil((usedMemory / maxMemory) * 100);
        extractedMetric.committedPercentage = maxMemory === 0 ? 0 : Math.ceil((committedMemory / maxMemory) * 100);

        return extractedMetric;
      }

      return {
        usedPercentage: 0,
        committedPercentage: 0,
      };
    }

    return {};
  }, [metrics, nodeId]);

  const { usedPercentage, committedPercentage, usedMemory, committedMemory, maxMemory } = extractedMetrics;
  let detail = (
    <span>Loading heap usage information...</span>
  );

  if (usedPercentage || committedPercentage) {
    if (Object.keys(extractedMetrics).length === 0) {
      detail = <span>Heap information unavailable.</span>;
    } else {
      detail = (
        <span>
          The JVM is using
          <strong> {NumberUtils.formatBytes(usedMemory)}</strong> of
          <strong> {NumberUtils.formatBytes(committedMemory)}</strong> heap space and will not attempt to use more than{' '}
          <strong> {NumberUtils.formatBytes(maxMemory)}</strong>.
        </span>
      );
    }
  }

  return detail;
};

export default JvmHeapUsageText;
