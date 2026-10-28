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
import useInputMetrics from 'hooks/useInputMetrics';
import type { InputMetricField, InputMetrics } from 'hooks/useInputMetrics';

// Loads the metrics of one input, e.g. for an expanded section, independent of the metrics requested for the visible columns.
const useSingleInputMetrics = (
  inputId: string,
  fields: Array<InputMetricField>,
): {
  metrics: InputMetrics | undefined;
  isInitialLoading: boolean;
  isError: boolean;
} => {
  const { metricsByInputId, isInitialLoading, isError } = useInputMetrics([inputId], fields);

  return { metrics: metricsByInputId[inputId], isInitialLoading, isError };
};

export default useSingleInputMetrics;
