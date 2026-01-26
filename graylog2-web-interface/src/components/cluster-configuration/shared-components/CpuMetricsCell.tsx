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

import { buildRatioIndicator } from './RatioIndicator';
import { MetricPlaceholder, MetricsColumn, MetricsRow, SecondaryText } from './NodeMetricsLayout';

type Props = {
  loadAverage?: number | null;
  cpuPercent?: number | null;
  warningThreshold?: number;
  dangerThreshold?: number;
};

const formatNumberValue = (value: number | undefined | null, suffix = '') =>
  value == null ? '' : `${NumberUtils.formatNumber(value)}${suffix}`;

const toRatio = (percent: number | undefined | null) => {
  if (percent == null) {
    return null;
  }

  return percent > 1 ? percent / 100 : percent;
};

const CpuMetricsCell = ({
  loadAverage = null,
  cpuPercent = null,
  warningThreshold = Number.NaN,
  dangerThreshold = Number.NaN,
}: Props) => {
  const loadLabel = formatNumberValue(loadAverage);
  const percentLabel = formatNumberValue(cpuPercent, '%');
  const percentIndicator =
    cpuPercent == null ? null : buildRatioIndicator(toRatio(cpuPercent), warningThreshold, dangerThreshold);

  if (!loadLabel && !percentLabel && !percentIndicator) {
    return <MetricPlaceholder />;
  }

  return (
    <MetricsColumn>
      {(percentIndicator || percentLabel) && <MetricsRow>{percentIndicator ?? <span>{percentLabel}</span>}</MetricsRow>}
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
