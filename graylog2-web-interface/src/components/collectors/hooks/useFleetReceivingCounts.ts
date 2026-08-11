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
import { useQuery } from '@tanstack/react-query';

import {
  COLLECTOR_FLEET_ID_FIELD,
  COLLECTOR_INSTANCE_UID_FIELD,
  COLLECTOR_LOG_RECEIVER_TYPE,
  COLLECTOR_RECEIVER_TYPE_FIELD,
} from 'components/collectors/common/fields';
import generateId from 'logic/generateId';
import Query, { createElasticsearchQueryString } from 'views/logic/queries/Query';
import type { RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import createSearch from 'views/logic/slices/createSearch';
import { startJob, executeJobResult } from 'views/logic/slices/executeJobResult';
import type { AggregationSearchType } from 'views/logic/queries/SearchType';

import {
  COUNT_SERIES,
  ONBOARDING_KEY_PREFIX,
  PREVIEW_RANGE_SECONDS,
  REFRESH_INTERVAL_MS,
  parsePivotCounts,
} from './useCollectorLogPreview';
import type { RawPivotResult } from './useCollectorLogPreview';

// A `values` bucket returns only buckets that exist, capped at this limit — instances beyond it
// (the lowest-count ones) read as "no messages yet". Generous enough for any realistic rollout
// batch while still bounding the aggregation.
const INSTANCE_BUCKET_LIMIT = 500;

type CountsSearch = {
  search: Search;
  ids: {
    queryId: string;
    searchTypeId: string;
  };
};

const countsTimerange: RelativeTimeRangeWithEnd = { type: 'relative', from: PREVIEW_RANGE_SECONDS };

const instanceCountsSearchType = (id: string): AggregationSearchType => ({
  id,
  // `pivot`, not `aggregation` — see the sourceCountsSearchType comment in useCollectorLogPreview.
  type: 'pivot',
  row_groups: [{ type: 'values', fields: [COLLECTOR_INSTANCE_UID_FIELD], limit: INSTANCE_BUCKET_LIMIT }],
  column_groups: [],
  series: COUNT_SERIES,
  sort: [],
  rollup: false,
  filter: undefined,
  filters: undefined,
  name: undefined,
  query: undefined,
  timerange: undefined,
  streams: [],
  stream_categories: [],
});

const buildCountsSearch = (fleetId: string): CountsSearch => {
  const ids = {
    queryId: generateId(),
    searchTypeId: generateId(),
  };

  // One aggregation over the whole fleet, bucketed per instance. The NOT clause keeps the
  // collectors' own logs out of the counts (same guard as the log preview's source query).
  const query = Query.builder()
    .id(ids.queryId)
    .query(
      createElasticsearchQueryString(
        `${COLLECTOR_FLEET_ID_FIELD}:"${fleetId}" AND NOT ${COLLECTOR_RECEIVER_TYPE_FIELD}:"${COLLECTOR_LOG_RECEIVER_TYPE}"`,
      ),
    )
    .timerange(countsTimerange)
    .searchTypes([instanceCountsSearchType(ids.searchTypeId)])
    .build();

  const search = Search.builder().newId().queries([query]).parameters([]).build();

  return { search, ids };
};

/**
 * Message counts per collector instance for one fleet (last 15 minutes), from a single search
 * execution — used to show whether each enrolling host is receiving data without issuing one
 * query per host. Returns undefined while loading or when the aggregation is unavailable.
 */
const useFleetReceivingCounts = (fleetId: string) => {
  const { data: created, error: createError } = useQuery<CountsSearch>({
    queryKey: [...ONBOARDING_KEY_PREFIX, 'fleet-receiving-search', fleetId],
    queryFn: async () => {
      const { search, ids } = buildCountsSearch(fleetId);
      const saved = await createSearch(search);

      return { search: saved, ids };
    },
    staleTime: Infinity,
  });

  const { data: counts, error: executeError } = useQuery({
    // eslint-disable-next-line @tanstack/query/exhaustive-deps -- created.search.id uniquely identifies the search; including the full object would cause spurious re-runs
    queryKey: [...ONBOARDING_KEY_PREFIX, 'fleet-receiving-results', created?.search?.id],
    enabled: !!created,
    refetchInterval: REFRESH_INTERVAL_MS,
    queryFn: async () => {
      if (!created) throw new Error('search not created yet');

      const { search, ids } = created;
      const jobIds = await startJob(search, [], SearchExecutionState.empty(), []);
      const { result } = await executeJobResult({ jobIds });

      const errors = result.errors ?? [];

      if (errors.length > 0) {
        throw new Error(errors[0].description ?? 'Search failed');
      }

      return parsePivotCounts(result.forId(ids.queryId)?.searchTypes?.[ids.searchTypeId] as RawPivotResult | undefined);
    },
  });

  return {
    counts,
    error: (createError ?? executeError ?? null) as Error | null,
  };
};

export default useFleetReceivingCounts;
