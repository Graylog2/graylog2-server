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

import { SystemIndexSetsMetrics } from '@graylog/server-api';

import { KEY_PREFIX } from '../fetchIndexSets';

export type IndexSetMetricField = 'index_count' | 'document_count' | 'size_bytes' | 'deflector_health' | 'field_count';

export type DeflectorHealth = 'Green' | 'Yellow' | 'Red';

export type IndexSetMetrics = {
  index_count?: number | null;
  document_count?: number | null;
  size_bytes?: number | null;
  deflector_health?: DeflectorHealth | null;
  field_count?: number | null;
};

export type IndexSetMetricsByIndexSetId = Record<string, IndexSetMetrics>;

const POLL_INTERVAL_MS = 60_000;

export const fetchIndexSetMetrics = (
  indexSetIds: Array<string>,
  fields: Array<IndexSetMetricField>,
): Promise<IndexSetMetricsByIndexSetId> =>
  SystemIndexSetsMetrics.getMetrics(indexSetIds, fields, { requestShouldExtendSession: false }).then(
    (response) => response.metrics as IndexSetMetricsByIndexSetId,
  );

const sortedUnique = <T extends string>(values: Array<T>): Array<T> => Array.from(new Set(values)).sort();

const useIndexSetMetrics = (
  indexSetIds: Array<string>,
  fields: Array<IndexSetMetricField>,
): {
  metricsByIndexSetId: IndexSetMetricsByIndexSetId;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
} => {
  const stableIds = sortedUnique(indexSetIds);
  const stableFields = sortedUnique(fields);

  const { data, isInitialLoading, isPlaceholderData, isError, refetch } = useQuery({
    queryKey: [...KEY_PREFIX, 'metrics', stableIds, stableFields],
    queryFn: () => fetchIndexSetMetrics(stableIds, stableFields),
    enabled: stableIds.length > 0 && stableFields.length > 0,
    placeholderData: keepPreviousData,
    refetchInterval: POLL_INTERVAL_MS,
  });

  return {
    metricsByIndexSetId: data ?? {},
    isLoading: isInitialLoading || isPlaceholderData,
    isError,
    refetch,
  };
};

export default useIndexSetMetrics;
