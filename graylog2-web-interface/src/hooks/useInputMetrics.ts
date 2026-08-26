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
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import Qs from 'qs';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';

export type InputMetricField = 'messages_per_stream' | 'extractor_count';

export type InputMetrics = {
  messages_per_stream?: Record<string, number>;
  extractor_count?: number;
};

export type InputMetricsByInputId = Record<string, InputMetrics>;

type InputMetricsResponse = {
  metrics: InputMetricsByInputId;
};

const POLL_INTERVAL_MS = 60_000;

export const fetchInputMetrics = (
  inputIds: Array<string>,
  fields: Array<InputMetricField>,
): Promise<InputMetricsResponse> => {
  const query = Qs.stringify({ input_ids: inputIds, fields }, { indices: false });

  return fetch('GET', qualifyUrl(`/system/inputs/metrics?${query}`));
};

const sortedUnique = <T extends string>(values: Array<T>): Array<T> => Array.from(new Set(values)).sort();

const useInputMetrics = (
  inputIds: Array<string>,
  fields: Array<InputMetricField>,
): {
  metricsByInputId: InputMetricsByInputId;
  isInitialLoading: boolean;
  isError: boolean;
} => {
  const stableIds = sortedUnique(inputIds);
  const stableFields = sortedUnique(fields);
  const enabled = stableIds.length > 0 && stableFields.length > 0;

  const { data, isInitialLoading, isError } = useQuery({
    queryKey: ['inputs', 'metrics', stableIds, stableFields],
    queryFn: () =>
      defaultOnError(
        fetchInputMetrics(stableIds, stableFields),
        'Loading input metrics failed with status',
        'Could not load input metrics',
      ),
    enabled,
    placeholderData: keepPreviousData,
    refetchInterval: POLL_INTERVAL_MS,
  });

  return {
    metricsByInputId: data?.metrics ?? {},
    isInitialLoading,
    isError,
  };
};

export default useInputMetrics;
