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

import { buildRatioIndicator, computeRatio } from '../../shared-components/RatioIndicator';
import { MetricPlaceholder, MetricsColumn, MetricsRow, SecondaryText } from '../../shared-components/NodeMetricsLayout';
import type { ClusterGraylogNode } from '../fetchClusterGraylogNodes';

type Props = {
  node: ClusterGraylogNode;
  warningThreshold?: number;
  dangerThreshold?: number;
};

const renderBufferRow = (
  label: string,
  usage: number | undefined | null,
  size: number | undefined | null,
  warningThreshold: number,
  dangerThreshold: number,
) => {
  const ratioIndicator = buildRatioIndicator(computeRatio(usage, size), warningThreshold, dangerThreshold);

  return (
    <MetricsRow key={label}>
      <span>{label}</span>
      {ratioIndicator ?? (
        <SecondaryText>
          <span>N/A</span>
        </SecondaryText>
      )}
    </MetricsRow>
  );
};

const BuffersMetricsCell = ({ node, warningThreshold = Number.NaN, dangerThreshold = Number.NaN }: Props) => {
  const rows = [
    {
      label: 'Input',
      usage: node.metrics?.bufferInputUsage,
      size: node.metrics?.bufferInputSize,
    },
    {
      label: 'Process',
      usage: node.metrics?.bufferProcessUsage,
      size: node.metrics?.bufferProcessSize,
    },
    {
      label: 'Output',
      usage: node.metrics?.bufferOutputUsage,
      size: node.metrics?.bufferOutputSize,
    },
  ];

  const hasBufferMetrics = rows.some(({ usage, size }) => computeRatio(usage, size) != null);

  if (!hasBufferMetrics) {
    return <MetricPlaceholder />;
  }

  return (
    <MetricsColumn>
      {rows.map(({ label, usage, size }) => renderBufferRow(label, usage, size, warningThreshold, dangerThreshold))}
    </MetricsColumn>
  );
};

export default BuffersMetricsCell;
