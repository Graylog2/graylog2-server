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
import React from 'react';

import type { ColumnRenderers, ColumnSchema } from 'components/common/EntityDataTable';

import type { ClusterGraylogNode as GraylogNode } from './fetchClusterGraylogNodes';
import BuffersMetricsCell from './cells/BuffersMetricsCell';
import HostnameCell from './cells/HostnameCell';
import LifecycleCell from './cells/LifecycleCell';
import LoadBalancerStatusCell from './cells/LoadBalancerStatusCell';
import ProcessingStateCell from './cells/ProcessingStateCell';
import ThroughputMetricsCell from './cells/ThroughputMetricsCell';

import CpuMetricsCell from '../shared-components/CpuMetricsCell';
import SizeAndRatioMetric from '../shared-components/SizeAndRatioMetric';

const JOURNAL_WARNING_THRESHOLD = 0.1;
const JOURNAL_DANGER_THRESHOLD = 0.4;
const BUFFER_WARNING_THRESHOLD = 0.95;
const JVM_WARNING_THRESHOLD = 0.95;
const CPU_WARNING_THRESHOLD = 0.7;
const CPU_DANGER_THRESHOLD = 0.9;

export const DEFAULT_VISIBLE_COLUMNS = [
  'hostname',
  'lifecycle',
  'cpu',
  'jvm',
  'buffers',
  'journal',
  'dataLakeJournal',
  'throughput',
  'is_processing',
  'lb_status',
];

export const createColumnDefinitions = (): Array<ColumnSchema> => [
  { id: 'cpu', title: 'CPU', isDerived: true, sortable: false },
  { id: 'jvm', title: 'JVM', isDerived: true, sortable: false },
  { id: 'buffers', title: 'Buffers', isDerived: true, sortable: false },
  { id: 'journal', title: 'Journal', isDerived: true, sortable: false },
  { id: 'dataLakeJournal', title: 'Data Lake Journal', isDerived: true, sortable: false },
  { id: 'throughput', title: 'Throughput', isDerived: true, sortable: false },
];

export const createColumnRenderers = (): ColumnRenderers<GraylogNode> => ({
  attributes: {
    hostname: {
      renderCell: (_value, entity) => <HostnameCell node={entity} />,
      minWidth: 300,
    },
    lifecycle: {
      renderCell: (_value, entity) => <LifecycleCell node={entity} />,
      staticWidth: 130,
    },
    is_processing: {
      renderCell: (_value, entity) => <ProcessingStateCell node={entity} />,
      staticWidth: 'matchHeader',
    },
    lb_status: {
      renderCell: (_value, entity) => <LoadBalancerStatusCell node={entity} />,
      staticWidth: 'matchHeader',
    },
    cpu: {
      renderCell: (_value, entity) => (
        <CpuMetricsCell
          cpuPercent={entity.metrics?.cpuPercent}
          warningThreshold={CPU_WARNING_THRESHOLD}
          dangerThreshold={CPU_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 130,
    },
    journal: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={entity.metrics?.journalSize}
          max={entity.metrics?.journalMaxSize}
          ratio={entity.metrics?.journalSizeRatio}
          warningThreshold={JOURNAL_WARNING_THRESHOLD}
          dangerThreshold={JOURNAL_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 130,
    },
    dataLakeJournal: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={entity.metrics?.dataLakeJournalSize}
          max={entity.metrics?.journalMaxSize}
          warningThreshold={JOURNAL_WARNING_THRESHOLD}
          dangerThreshold={JOURNAL_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 'matchHeader',
    },
    jvm: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={entity.metrics?.jvmMemoryHeapUsed}
          max={entity.metrics?.jvmMemoryHeapMax}
          warningThreshold={JVM_WARNING_THRESHOLD}
        />
      ),
      staticWidth: 130,
    },
    buffers: {
      renderCell: (_value, entity) => <BuffersMetricsCell node={entity} warningThreshold={BUFFER_WARNING_THRESHOLD} />,
      staticWidth: 130,
    },
    throughput: {
      renderCell: (_value, entity) => <ThroughputMetricsCell node={entity} />,
      staticWidth: 'matchHeader',
    },
  },
});
