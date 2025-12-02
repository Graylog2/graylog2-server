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
  loadAverage: number | undefined | null;
};

const formatNumberValue = (value: number | undefined | null, suffix = '') =>
  value == null ? '' : `${NumberUtils.formatNumber(value)}${suffix}`;

const CpuMetricsCell = ({ loadAverage }: Props) => {
  const loadLabel = formatNumberValue(loadAverage);

  if (!loadLabel) {
    return <MetricPlaceholder />;
  }

  return (
    <MetricsColumn>
      <MetricsRow>
        <span>Load (1m)</span>
        <SecondaryText>
          <span>{loadLabel}</span>
        </SecondaryText>
      </MetricsRow>
    </MetricsColumn>
  );
};

export default CpuMetricsCell;
