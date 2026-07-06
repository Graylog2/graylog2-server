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

// Known open-source fields. Plugins (e.g. the enterprise failure_count column) may contribute
// additional field names via the `components.streams.overview.tableElements` extension, so the
// hook accepts `Array<string>` at the boundary.
export type StreamMetricField =
  | 'message_count'
  | 'avg_processing_time_ms'
  | 'max_processing_time_ms'
  | 'associated_inputs'
  | 'pipelines'
  | 'routing_pipelines';

export type TypedInputId = { id: string; type: string };

export type StreamMetrics = {
  message_count?: number;
  avg_processing_time_ms?: number;
  max_processing_time_ms?: number;
  associated_inputs?: Array<TypedInputId>;
  pipelines?: Array<string>;
  routing_pipelines?: Array<string>;
  [extensionField: string]: unknown;
};

export type StreamMetricsByStreamId = Record<string, StreamMetrics>;

type StreamMetricsResponse = {
  metrics: StreamMetricsByStreamId;
};

// TODO: swap for `Streams.getMetrics` from `@graylog/server-api` once the swagger
// spec regenerates (the backend resource exists; the TS client has not been
// regenerated yet).
const POLL_INTERVAL_MS = 60_000;

export const fetchStreamMetrics = (
  streamIds: Array<string>,
  fields: Array<StreamMetricField | string>,
): Promise<StreamMetricsResponse> => {
  const query = Qs.stringify({ stream_ids: streamIds, fields }, { indices: false });

  return fetch('GET', qualifyUrl(`/streams/metrics?${query}`));
};

const sortedUnique = <T extends string>(values: Array<T>): Array<T> => Array.from(new Set(values)).sort();

const useStreamMetrics = (
  streamIds: Array<string>,
  fields: Array<StreamMetricField | string>,
): {
  metricsByStreamId: StreamMetricsByStreamId;
  isInitialLoading: boolean;
  isError: boolean;
} => {
  const stableIds = sortedUnique(streamIds);
  const stableFields = sortedUnique(fields);
  const enabled = stableIds.length > 0 && stableFields.length > 0;

  const { data, isInitialLoading, isError } = useQuery({
    queryKey: ['streams', 'metrics', stableIds, stableFields],
    queryFn: () =>
      defaultOnError(
        fetchStreamMetrics(stableIds, stableFields),
        'Loading stream metrics failed with status',
        'Could not load stream metrics',
      ),
    enabled,
    placeholderData: keepPreviousData,
    refetchInterval: POLL_INTERVAL_MS,
  });

  return {
    metricsByStreamId: data?.metrics ?? {},
    isInitialLoading,
    isError,
  };
};

export default useStreamMetrics;
