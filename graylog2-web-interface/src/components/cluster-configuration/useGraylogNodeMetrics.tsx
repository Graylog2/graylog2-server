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
import { useEffect } from 'react';

import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { useStore } from 'stores/connect';

type Props = {
  nodeId: string;
};
const metricNames = {
  journalAppend1SecRate: 'org.graylog2.journal.append.1-sec-rate',
  journalRead1SecRate: 'org.graylog2.journal.read.1-sec-rate',
  journalSegments: 'org.graylog2.journal.segments',
  journalEntriesUncommitted: 'org.graylog2.journal.entries-uncommitted',
  journalSize: 'org.graylog2.journal.size',
  journalmaxSize: 'org.graylog2.journal.size-limit',
  journalSizeRatio: 'org.graylog2.journal.utilization-ratio',
  jvmMemoryHeapUsed: 'jvm.memory.heap.used',
  jvmMemoryHeapCommitted: 'jvm.memory.heap.committed',
  jvmMemoryHeapmaxMemory: 'jvm.memory.heap.max',
};

const useGraylogNodeMetrics = ({ nodeId }: Props): Record<string, unknown> => {
  const { metrics } = useStore(MetricsStore);

  useEffect(() => {
    Object.keys(metricNames).forEach((metricShortName) => MetricsActions.add(nodeId, metricNames[metricShortName]));

    return () => {
      Object.keys(metricNames).forEach((metricShortName) =>
        MetricsActions.remove(nodeId, metricNames[metricShortName]),
      );
    };
  }, [nodeId]);

  const nodeMetrics = metrics?.[nodeId];
  console.log('nodeMetrics', nodeMetrics);

  if (!nodeMetrics) {
    return {} as Record<string, unknown>;
  }

  return MetricsExtractor.getValuesForNode(nodeMetrics, metricNames);
};

export default useGraylogNodeMetrics;
