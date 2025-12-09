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

import { MetricPlaceholder, MetricsColumn, MetricsRow, SecondaryText } from '../../shared-components/NodeMetricsLayout';

type Props = {
  totalIndexed: number | undefined | null;
  indexTimeInMillis: number | undefined | null;
};

const formatNumberValue = (value: number | undefined | null, suffix = '') =>
  value == null ? '' : `${NumberUtils.formatNumber(value)}${suffix}`;

const formatIndexLatency = (total: number | undefined | null, timeInMillis: number | undefined | null) => {
  if (total == null || timeInMillis == null || timeInMillis === 0) {
    return '';
  }

  const latency = timeInMillis / total;

  return `${NumberUtils.formatNumber(latency)} ms/op`;
};

const IndexingMetricsCell = ({ totalIndexed, indexTimeInMillis }: Props) => {
  const totalIndexedLabel = formatNumberValue(totalIndexed);
  const indexingLatencyLabel = formatIndexLatency(totalIndexed, indexTimeInMillis);
  const hasContent = totalIndexedLabel || indexingLatencyLabel;

  if (!hasContent) {
    return <MetricPlaceholder />;
  }

  return (
    <MetricsColumn>
      {totalIndexedLabel && (
        <MetricsRow>
          <span>Docs</span>
          <SecondaryText>
            <span>{totalIndexedLabel}</span>
          </SecondaryText>
        </MetricsRow>
      )}
      {indexingLatencyLabel && (
        <MetricsRow>
          <span>Latency</span>
          <SecondaryText>
            <span>{indexingLatencyLabel}</span>
          </SecondaryText>
        </MetricsRow>
      )}
    </MetricsColumn>
  );
};

export default IndexingMetricsCell;
