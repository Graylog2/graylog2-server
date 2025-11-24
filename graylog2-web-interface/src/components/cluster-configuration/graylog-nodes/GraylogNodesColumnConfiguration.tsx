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

import { Link } from 'components/common/router';
import type { ColumnRenderers, ColumnSchema } from 'components/common/EntityDataTable';
import Routes from 'routing/Routes';
import NumberUtils from 'util/NumberUtils';

import type { GraylogNode } from './useClusterGraylogNodes';

import SizeAndRatioMetric from '../shared-components/SizeAndRatioMetric';
import { buildRatioIndicator, computeRatio } from '../shared-components/RatioIndicator';
import { MetricsColumn, MetricsRow, NodePrimary, SecondaryText, StyledLabel } from '../shared-components/NodeMetricsLayout';

const JOURNAL_WARNING_THRESHOLD = 0.25;
const JOURNAL_DANGER_THRESHOLD = 0.5;
const JVM_WARNING_THRESHOLD = 0.7;
const JVM_DANGER_THRESHOLD = 0.9;
const BUFFER_WARNING_THRESHOLD = 0.7;
const BUFFER_DANGER_THRESHOLD = 0.9;

const renderBufferRow = (
  label: string,
  usage: number | undefined | null,
  size: number | undefined | null,
) => {
  const ratioIndicator = buildRatioIndicator(
    computeRatio(usage, size),
    BUFFER_WARNING_THRESHOLD,
    BUFFER_DANGER_THRESHOLD,
  );

  return (
    <MetricsRow key={label}>
      <span>{label}</span>
      {ratioIndicator}
    </MetricsRow>
  );
};

const formatThroughput = (value: number | undefined | null) => {
  if (value == null) {
    return '';
  }

  const formatted = NumberUtils.formatNumber(value);

  return formatted ? `${formatted} msg/s` : '';
};

const renderThroughputRow = (label: string, value: number | undefined | null) => (
  <MetricsRow key={label}>
    <span>{label}</span>
    <span>{formatThroughput(value)}</span>
  </MetricsRow>
);

const getNodeDisplayName = (node: GraylogNode) => {
  const nodeNameParts = [node.short_node_id, node.hostname].filter(Boolean);

  if (nodeNameParts.length) {
    return nodeNameParts.join(' / ');
  }

  return node.node_id ?? node.hostname ?? node.id;
};

export const DEFAULT_VISIBLE_COLUMNS = [
  'hostname',
  'lifecycle',
  'jvm',
  'buffers',
  'journal',
  'dataLakeJournal',
  'throughput',
  'is_processing',
  'lb_status',
] as const;

export const createColumnDefinitions = (): Array<ColumnSchema> => [
  { id: 'hostname', title: 'Node', sortable: true },
  { id: 'lifecycle', title: 'State', sortable: true },
  { id: 'jvm', title: 'JVM', isDerived: true, sortable: false },
  { id: 'buffers', title: 'Buffers', isDerived: true, sortable: false },
  { id: 'journal', title: 'Journal', isDerived: true, sortable: false },
  { id: 'dataLakeJournal', title: 'Data Lake Journal', isDerived: true, sortable: false },
  { id: 'throughput', title: 'Throughput', isDerived: true, sortable: false },
  { id: 'is_processing', title: 'Message Processing', sortable: true },
  { id: 'lb_status', title: 'Load Balancer', sortable: true },
];

export const createColumnRenderers = (): ColumnRenderers<GraylogNode> => ({
  attributes: {
    hostname: {
      renderCell: (_value, entity) => {
        const nodeId = entity.node_id;
        const nodeName = getNodeDisplayName(entity);

        return (
          <NodePrimary>
            {nodeId ? <Link to={Routes.SYSTEM.CLUSTER.NODE_SHOW(nodeId)}>{nodeName}</Link> : nodeName}
            {entity.is_leader && (
              <SecondaryText>
                <StyledLabel bsSize="xs">Leader</StyledLabel>
              </SecondaryText>
            )}
          </NodePrimary>
        );
      },
    },
    lifecycle: {
      renderCell: (_value, entity) => {
        const lifecycleStatus = entity.lifecycle?.toUpperCase();

        if (!lifecycleStatus) {
          return null;
        }

        return (
          <MetricsColumn>
            <MetricsRow>
              <StyledLabel
                bsStyle={lifecycleStatus === 'RUNNING' ? 'success' : 'warning'}
                bsSize="xs"
                title={lifecycleStatus}
                aria-label={lifecycleStatus}>
                {lifecycleStatus}
              </StyledLabel>
            </MetricsRow>
          </MetricsColumn>
        );
      },
    },
    is_processing: {
      renderCell: (_value, entity) => {
        if (entity.is_processing == null) {
          return null;
        }

        const status = entity.is_processing ? 'ENABLED' : 'DISABLED';

        return (
          <MetricsColumn>
            <MetricsRow>
              <StyledLabel bsStyle={entity.is_processing ? 'success' : 'warning'} bsSize="xs" aria-label={`Message processing ${status}`}>
                {status}
              </StyledLabel>
            </MetricsRow>
          </MetricsColumn>
        );
      },
    },
    lb_status: {
      renderCell: (_value, entity) => {
        const status = entity.lb_status?.toUpperCase();

        if (!status) {
          return null;
        }

        return (
          <MetricsColumn>
            <MetricsRow>
              <StyledLabel bsStyle={status === 'ALIVE' ? 'success' : 'warning'} bsSize="xs" aria-label={`Load balancer ${status}`}>
                {status}
              </StyledLabel>
            </MetricsRow>
          </MetricsColumn>
        );
      },
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
    },
    jvm: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={entity.metrics?.jvmMemoryHeapUsed}
          max={entity.metrics?.jvmMemoryHeapMax}
          warningThreshold={JVM_WARNING_THRESHOLD}
          dangerThreshold={JVM_DANGER_THRESHOLD}
        />
      ),
    },
    buffers: {
      renderCell: (_value, entity) => {
        const rows = [
          {
            label: 'Input',
            usage: entity.metrics?.bufferInputUsage,
            size: entity.metrics?.bufferInputSize,
          },
          {
            label: 'Process',
            usage: entity.metrics?.bufferProcessUsage,
            size: entity.metrics?.bufferProcessSize,
          },
          {
            label: 'Output',
            usage: entity.metrics?.bufferOutputUsage,
            size: entity.metrics?.bufferOutputSize,
          },
        ];

        return (
          <MetricsColumn>
            {rows.map(({ label, usage, size }) => renderBufferRow(label, usage, size))}
          </MetricsColumn>
        );
      },
    },
    throughput: {
      renderCell: (_value, entity) => {
        const rows = [
          { label: 'In', value: entity.metrics?.throughputIn },
          { label: 'Out', value: entity.metrics?.throughputOut },
        ];

        return <MetricsColumn>{rows.map(({ label, value }) => renderThroughputRow(label, value))}</MetricsColumn>;
      },
    },
  },
});
