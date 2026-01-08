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

import NumberUtils from 'util/NumberUtils';

import type { ClusterGraylogNode } from '../fetchClusterGraylogNodes';
import { MetricPlaceholder, MetricsColumn, MetricsRow, SecondaryText } from '../../shared-components/NodeMetricsLayout';

type Props = {
  node: ClusterGraylogNode;
};

const formatThroughput = (value: number | undefined | null) => {
  if (value == null) {
    return '';
  }

  const formatted = NumberUtils.formatNumber(value);

  return formatted ? `${formatted} msg/s` : '';
};

const renderThroughputRow = (label: string, formattedValue: string | undefined | null) => (
  <MetricsRow key={label}>
    <span>{label}</span>
    <SecondaryText>
      <span>{formattedValue || 'N/A'}</span>
    </SecondaryText>
  </MetricsRow>
);

const ThroughputMetricsCell = ({ node }: Props) => {
  const rows = [
    { label: 'In', value: formatThroughput(node.metrics?.throughputIn) },
    { label: 'Out', value: formatThroughput(node.metrics?.throughputOut) },
  ];

  const hasThroughput = rows.some(({ value }) => value);

  if (!hasThroughput) {
    return <MetricPlaceholder />;
  }

  return <MetricsColumn>{rows.map(({ label, value }) => renderThroughputRow(label, value))}</MetricsColumn>;
};

export default ThroughputMetricsCell;
