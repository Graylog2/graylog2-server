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
import * as React from 'react';

import { IconButton, Spinner } from 'components/common';

import metricCellState from './metricCellState';

import { useIndexSetMetricsFor } from '../IndexSetMetricsContext';
import { METRIC_COLUMN_TITLES } from '../metricColumns';
import type { IndexSetMetricField, IndexSetMetrics } from '../hooks/useIndexSetMetrics';
import type { IndexSetEntity } from '../types';

type Props<F extends IndexSetMetricField> = {
  indexSet: IndexSetEntity;
  field: F;
  renderValue: (value: NonNullable<IndexSetMetrics[F]>) => React.ReactNode;
};

const MetricCell = <F extends IndexSetMetricField>({ indexSet, field, renderValue }: Props<F>) => {
  const { metrics, isLoading, isError, refetch } = useIndexSetMetricsFor(indexSet.id);
  const value = metrics?.[field];
  const title = METRIC_COLUMN_TITLES[field];

  const state = metricCellState({
    hasValue: value !== undefined && value !== null,
    isLoading,
    isError,
  });

  switch (state) {
    case 'value':
      return renderValue(value as NonNullable<IndexSetMetrics[F]>);
    case 'loading':
      return <Spinner size="xs" />;
    case 'error':
      return (
        <IconButton
          name="warning"
          title="Could not load metrics. Click to retry"
          ariaLabel={`Retry loading ${title.toLowerCase()} for index set ${indexSet.title}`}
          size="xsmall"
          onClick={() => refetch()}
        />
      );
    default:
      return null;
  }
};

export default MetricCell;
