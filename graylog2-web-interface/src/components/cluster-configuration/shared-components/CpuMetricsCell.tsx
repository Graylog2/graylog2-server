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

import { MetricPlaceholder, MetricsColumn, MetricsRow, SecondaryText } from './NodeMetricsLayout';

type Props = {
  loadAverage?: number | null;
  cpuPercent?: number | null;
};

const formatNumberValue = (value: number | undefined | null, suffix = '') =>
  value == null ? '' : `${NumberUtils.formatNumber(value)}${suffix}`;

const CpuMetricsCell = ({ loadAverage = null, cpuPercent = null }: Props) => {
  const loadLabel = formatNumberValue(loadAverage);
  const percentLabel = formatNumberValue(cpuPercent, '%');

  if (!loadLabel && !percentLabel) {
    return <MetricPlaceholder />;
  }

  return (
    <MetricsColumn>
      {percentLabel && (
        <MetricsRow>
          <span>{percentLabel}</span>
        </MetricsRow>
      )}
      {loadLabel && (
        <MetricsRow>
          <span>Load (1m)</span>
          <SecondaryText>
            <span>{loadLabel}</span>
          </SecondaryText>
        </MetricsRow>
      )}
    </MetricsColumn>
  );
};

export default CpuMetricsCell;
