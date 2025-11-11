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
import styled from 'styled-components';

import { Label } from 'components/bootstrap';
import { Link } from 'components/common/router';
import type { Column, ColumnRenderers } from 'components/common/EntityDataTable';
import Routes from 'routing/Routes';
import NumberUtils from 'util/NumberUtils';

import RatioIndicator from './RatioIndicator';
import { MetricsColumn, MetricsRow, SecondaryText } from './NodeMetricsLayout';
import type { GraylogNode } from './useClusterGraylogNodes';

const NodePrimary = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
`;

const StyledLabel = styled(Label)`
  display: inline-flex;
`;

const JOURNAL_WARNING_THRESHOLD = 0.25;
const JOURNAL_DANGER_THRESHOLD = 0.5;
const JVM_WARNING_THRESHOLD = 0.7;
const JVM_DANGER_THRESHOLD = 0.9;
const BUFFER_WARNING_THRESHOLD = 0.6;
const BUFFER_DANGER_THRESHOLD = 0.8;

const computeRatio = (current: number | undefined | null, max: number | undefined | null) => {
  if (current === undefined || current === null || max === undefined || max === null || max === 0) {
    return undefined;
  }

  return current / max;
};

const renderRatioIndicator = (ratio: number | undefined | null, warning: number, danger: number) =>
  ratio === undefined || ratio === null ? null : (
    <RatioIndicator ratio={ratio} warningThreshold={warning} dangerThreshold={danger} />
  );


const renderBufferRow = (
  label: string,
  usage: number | undefined | null,
  size: number | undefined | null,
) => {
  const ratioIndicator = renderRatioIndicator(computeRatio(usage, size), BUFFER_WARNING_THRESHOLD, BUFFER_DANGER_THRESHOLD);

  return (
    <MetricsRow key={label}>
      <span>{label}</span>
      {ratioIndicator}
    </MetricsRow>
  );
};

const formatThroughput = (value: number | undefined | null) => {
  if (value === undefined || value === null) {
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

export const DEFAULT_VISIBLE_COLUMNS = ['node', 'state', 'messageProcessing', 'loadBalancer', 'journal', 'dataLakeJournal', 'jvm', 'buffers', 'throughput'] as const;

export const createColumnDefinitions = (): Array<Column> => [
  { id: 'node', title: 'Node' },
  { id: 'state', title: 'State' },
  { id: 'messageProcessing', title: 'Message Processing' },
  { id: 'loadBalancer', title: 'Load Balancer' },
  { id: 'journal', title: 'Journal' },
  { id: 'dataLakeJournal', title: 'Data Lake Journal' },
  { id: 'jvm', title: 'JVM' },
  { id: 'buffers', title: 'Buffers' },
  { id: 'throughput', title: 'Throughput' },
];

export const createColumnRenderers = (): ColumnRenderers<GraylogNode> => ({
  attributes: {
    node: {
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
    state: {
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
    messageProcessing: {
      renderCell: (_value, entity) => {
        if (entity.is_processing === undefined || entity.is_processing === null) {
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
    loadBalancer: {
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
      renderCell: (_value, entity) => {
        const journalCurrent = entity.metrics?.journalSize;
        const journalMax = entity.metrics?.journalMaxSize;
        const ratio = entity.metrics?.journalSizeRatio ?? computeRatio(journalCurrent, journalMax);
        const currentLabel =
          journalCurrent === undefined || journalCurrent === null ? '' : NumberUtils.formatBytes(journalCurrent);
        const maxLabel = journalMax === undefined || journalMax === null ? '' : NumberUtils.formatBytes(journalMax);
        const sizeLabel = [currentLabel, maxLabel].filter(Boolean).join(' / ');
        const ratioIndicator = renderRatioIndicator(ratio, JOURNAL_WARNING_THRESHOLD, JOURNAL_DANGER_THRESHOLD);

        return (
          <MetricsColumn>
            {sizeLabel && (
              <MetricsRow>
                <span>{sizeLabel}</span>
              </MetricsRow>
            )}
            {ratioIndicator && <MetricsRow>{ratioIndicator}</MetricsRow>}
          </MetricsColumn>
        );
      },
    },
    dataLakeJournal: {
      renderCell: (_value, entity) => {
        const current = entity.metrics?.dataLakeJournalSize;
        const max = entity.metrics?.journalMaxSize;
        const ratio = computeRatio(current, max);
        const currentLabel = current === undefined || current === null ? '' : NumberUtils.formatBytes(current);
        const maxLabel = max === undefined || max === null ? '' : NumberUtils.formatBytes(max);
        const sizeLabel = [currentLabel, maxLabel].filter(Boolean).join(' / ');
        const ratioIndicator = renderRatioIndicator(ratio, JOURNAL_WARNING_THRESHOLD, JOURNAL_DANGER_THRESHOLD);

        return (
          <MetricsColumn>
            {sizeLabel && (
              <MetricsRow>
                <span>{sizeLabel}</span>
              </MetricsRow>
            )}
            {ratioIndicator && <MetricsRow>{ratioIndicator}</MetricsRow>}
          </MetricsColumn>
        );
      },
    },
    jvm: {
      renderCell: (_value, entity) => {
        const heapUsed = entity.metrics?.jvmMemoryHeapUsed;
        const heapMax = entity.metrics?.jvmMemoryHeapMax;
        const ratio = computeRatio(heapUsed, heapMax);
        const usedLabel = heapUsed === undefined || heapUsed === null ? '' : NumberUtils.formatBytes(heapUsed);
        const maxLabel = heapMax === undefined || heapMax === null ? '' : NumberUtils.formatBytes(heapMax);
        const sizeLabel = [usedLabel, maxLabel].filter(Boolean).join(' / ');
        const ratioIndicator = renderRatioIndicator(ratio, JVM_WARNING_THRESHOLD, JVM_DANGER_THRESHOLD);

        return (
          <MetricsColumn>
            {sizeLabel && (
              <MetricsRow>
                <span>{sizeLabel}</span>
              </MetricsRow>
            )}
            {ratioIndicator && <MetricsRow>{ratioIndicator}</MetricsRow>}
          </MetricsColumn>
        );
      },
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
