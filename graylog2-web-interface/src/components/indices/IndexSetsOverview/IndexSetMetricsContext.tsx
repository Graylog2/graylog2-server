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
import { createContext, useContext } from 'react';

import { singleton } from 'logic/singleton';

import useIndexSetMetrics from './hooks/useIndexSetMetrics';
import type { IndexSetMetricField, IndexSetMetrics, IndexSetMetricsByIndexSetId } from './hooks/useIndexSetMetrics';

type IndexSetMetricsContextValue = {
  metricsByIndexSetId: IndexSetMetricsByIndexSetId;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

const EMPTY_CONTEXT: IndexSetMetricsContextValue = {
  metricsByIndexSetId: {},
  isLoading: false,
  isError: false,
  refetch: () => {},
};

const IndexSetMetricsContext = singleton('contexts.IndexSetMetricsContext', () =>
  createContext<IndexSetMetricsContextValue>(EMPTY_CONTEXT),
);

type Props = React.PropsWithChildren<{
  indexSetIds: Array<string>;
  fields: Array<IndexSetMetricField>;
}>;

export const IndexSetMetricsProvider = ({ indexSetIds, fields, children = undefined }: Props) => {
  const value: IndexSetMetricsContextValue = useIndexSetMetrics(indexSetIds, fields);

  return <IndexSetMetricsContext.Provider value={value}>{children}</IndexSetMetricsContext.Provider>;
};

export const useIndexSetMetricsFor = (
  indexSetId: string,
): {
  metrics: IndexSetMetrics | undefined;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
} => {
  const { metricsByIndexSetId, isLoading, isError, refetch } = useContext(IndexSetMetricsContext);

  return {
    metrics: metricsByIndexSetId[indexSetId],
    isLoading,
    isError,
    refetch,
  };
};

export default IndexSetMetricsContext;
