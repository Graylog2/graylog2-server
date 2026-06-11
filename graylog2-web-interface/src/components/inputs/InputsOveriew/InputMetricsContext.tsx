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

import useInputMetrics from 'hooks/useInputMetrics';
import type { InputMetricField, InputMetrics, InputMetricsByInputId } from 'hooks/useInputMetrics';

type InputMetricsContextValue = {
  metricsByInputId: InputMetricsByInputId;
  isInitialLoading: boolean;
  isError: boolean;
  isFieldRequested: (field: InputMetricField) => boolean;
};

const EMPTY_CONTEXT: InputMetricsContextValue = {
  metricsByInputId: {},
  isInitialLoading: false,
  isError: false,
  isFieldRequested: () => false,
};

const InputMetricsContext = createContext<InputMetricsContextValue>(EMPTY_CONTEXT);

type Props = React.PropsWithChildren<{
  inputIds: Array<string>;
  fields: Array<InputMetricField>;
}>;

export const InputMetricsProvider = ({ inputIds, fields, children = undefined }: Props) => {
  const { metricsByInputId, isInitialLoading, isError } = useInputMetrics(inputIds, fields);
  const requestedFields = new Set(fields);

  const value: InputMetricsContextValue = {
    metricsByInputId,
    isInitialLoading,
    isError,
    isFieldRequested: (field) => requestedFields.has(field),
  };

  return <InputMetricsContext.Provider value={value}>{children}</InputMetricsContext.Provider>;
};

export const useInputMetricsContext = (): InputMetricsContextValue => useContext(InputMetricsContext);

export const useInputMetricsFor = (
  inputId: string,
): {
  metrics: InputMetrics | undefined;
  isInitialLoading: boolean;
  isError: boolean;
} => {
  const { metricsByInputId, isInitialLoading, isError } = useContext(InputMetricsContext);

  return {
    metrics: metricsByInputId[inputId],
    isInitialLoading,
    isError,
  };
};

export default InputMetricsContext;
