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
  COLLECTOR_INSTANCE_UID_FIELD,
  COLLECTOR_LOG_RECEIVER_TYPE,
  COLLECTOR_RECEIVER_TYPE_FIELD,
  COLLECTOR_SOURCE_ID_FIELD,
  COLLECTOR_SYSTEM_LOGS_STREAM_ID,
} from 'components/collectors/common/fields';
import generateId from 'logic/generateId';
import Query, { createElasticsearchQueryString, filtersForQuery } from 'views/logic/queries/Query';
import type { RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import createSearch from 'views/logic/slices/createSearch';
import { startJob, executeJobResult } from 'views/logic/slices/executeJobResult';
import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import type { MessagesSearchType, AggregationSearchType } from 'views/logic/queries/SearchType';

// Deliberately outside the 'collectors' prefix: useCollectorsMutations invalidates
// ['collectors'] wholesale on every mutation, which would re-create the backend search.
export const ONBOARDING_KEY_PREFIX = ['collector-onboarding'];

const REFRESH_INTERVAL_MS = 5000;
const PREVIEW_RANGE_SECONDS = 900; // last 15 minutes
const PREVIEW_MESSAGE_LIMIT = 10;
// A `values` bucket returns only buckets that exist, capped at this limit, so the limit must exceed
// the fleet's source count: absent-from-the-result is read as zero, and truncated buckets would be
// the lowest-count sources. Onboarding preconfigures 4 sources and fleets are not expected to pass
// ~50, so 100 is roughly double the realistic ceiling while staying a real guardrail.
const SOURCE_BUCKET_LIMIT = 100;

// `AggregationSearchType` requires `field` on every series, but a `count` series has none
// (`Count.field()` is Optional server-side). Widening that shared views type is deferred, so the
// series is asserted here instead. Do not "fix" this by adding a field: `count(<field>)` counts
// occurrences of that field and is a different aggregation from `count()`.
const COUNT_SERIES = [{ id: 'count()', type: 'count' }] as AggregationSearchType['series'];

export type PreviewMessage = {
  id: string;
  timestamp: string;
  text: string;
};

export type LogPreview = {
  messages: Array<PreviewMessage>;
  total: number;
};

type PreviewSearch = {
  search: Search;
  ids: {
    selfQueryId: string;
    selfSearchTypeId: string;
    sourceQueryId: string;
    sourceSearchTypeId: string;
    sourceCountsSearchTypeId: string;
  };
};

const messagesSearchType = (id: string): MessagesSearchType => ({
  id,
  type: 'messages',
  limit: PREVIEW_MESSAGE_LIMIT,
  offset: 0,
  sort: [new MessageSortConfig('timestamp', Direction.Descending)],
  decorators: [],
  filter: undefined,
  filters: undefined,
  name: undefined,
  query: undefined,
  timerange: undefined,
  streams: [],
  stream_categories: [],
});

const sourceCountsSearchType = (id: string): AggregationSearchType => ({
  id,
  // `pivot`, not `aggregation`: the wire discriminator is `Pivot.NAME` server-side. `aggregation` is
  // only the frontend's `PluggableSearchType` key — it coincides with the wire name for `messages`
  // but not here. An unrecognised type deserialises to `SearchType.Fallback`, whose `filters` is a
  // plain nullable field, and the search filter normalizer then NPEs on it and fails the whole search.
  type: 'pivot',
  row_groups: [{ type: 'values', fields: [COLLECTOR_SOURCE_ID_FIELD], limit: SOURCE_BUCKET_LIMIT }],
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

const previewTimerange: RelativeTimeRangeWithEnd = { type: 'relative', from: PREVIEW_RANGE_SECONDS };

const buildPreviewSearch = (instanceUid: string): PreviewSearch => {
  const ids = {
    selfQueryId: generateId(),
    selfSearchTypeId: generateId(),
    sourceQueryId: generateId(),
    sourceSearchTypeId: generateId(),
    sourceCountsSearchTypeId: generateId(),
  };

  // Self-logs live in the dedicated (system-scoped) collector logs stream.
  const selfLogsQuery = Query.builder()
    .id(ids.selfQueryId)
    .query(createElasticsearchQueryString(`${COLLECTOR_INSTANCE_UID_FIELD}:"${instanceUid}"`))
    .timerange(previewTimerange)
    .filter(filtersForQuery([COLLECTOR_SYSTEM_LOGS_STREAM_ID]))
    .searchTypes([messagesSearchType(ids.selfSearchTypeId)])
    .build();

  // Source logs: unscoped search. The NOT clause guards against self-logs leaking in if a
  // user routes the system stream into a searchable scope.
  const sourceLogsQuery = Query.builder()
    .id(ids.sourceQueryId)
    .query(
      createElasticsearchQueryString(
        `${COLLECTOR_INSTANCE_UID_FIELD}:"${instanceUid}" AND NOT ${COLLECTOR_RECEIVER_TYPE_FIELD}:"${COLLECTOR_LOG_RECEIVER_TYPE}"`,
      ),
    )
    .timerange(previewTimerange)
    .searchTypes([messagesSearchType(ids.sourceSearchTypeId), sourceCountsSearchType(ids.sourceCountsSearchTypeId)])
    .build();

  const search = Search.builder().newId().queries([sourceLogsQuery, selfLogsQuery]).parameters([]).build();

  return { search, ids };
};

type RawResultMessage = { message: { _id: string; timestamp: string; message: unknown } };
type RawMessagesResult = { messages?: Array<RawResultMessage>; total?: number };
type RawPivotValue = { source: string; value: unknown };
type RawPivotRow = { source: string; key: Array<string>; values?: Array<RawPivotValue> };
type RawPivotResult = { rows?: Array<RawPivotRow> };

const toPreview = (searchTypeResult: RawMessagesResult | undefined): LogPreview => ({
  messages: (searchTypeResult?.messages ?? []).map((m) => ({
    id: m.message._id,
    timestamp: m.message.timestamp,
    text: String(m.message.message ?? ''),
  })),
  total: searchTypeResult?.total ?? 0,
});

// A missing result means the aggregation was unavailable, which must stay distinguishable from
// "every source produced nothing" — the caller falls back to the aggregate status in that case.
const toSourceCounts = (searchTypeResult: RawPivotResult | undefined): Record<string, number> | undefined => {
  if (!searchTypeResult?.rows) {
    return undefined;
  }

  return Object.fromEntries(
    searchTypeResult.rows
      .filter((row) => row.source === 'leaf' && row.key.length > 0)
      .map((row) => [row.key[0], Number(row.values?.find((value) => value.source === 'row-leaf')?.value ?? 0)]),
  );
};

const useCollectorLogPreview = (instanceUid: string) => {
  const { data: created, error: createError } = useQuery<PreviewSearch>({
    queryKey: [...ONBOARDING_KEY_PREFIX, 'preview-search', instanceUid],
    queryFn: async () => {
      const { search, ids } = buildPreviewSearch(instanceUid);
      const saved = await createSearch(search);

      return { search: saved, ids };
    },
    staleTime: Infinity,
  });

  const {
    data: results,
    error: executeError,
    isLoading,
  } = useQuery({
    // eslint-disable-next-line @tanstack/query/exhaustive-deps -- created.search.id uniquely identifies the search; including the full object would cause spurious re-runs
    queryKey: [...ONBOARDING_KEY_PREFIX, 'preview-results', created?.search?.id],
    enabled: !!created,
    refetchInterval: REFRESH_INTERVAL_MS,
    queryFn: async () => {
      if (!created) throw new Error('search not created yet');

      const { search, ids } = created;
      const jobIds = await startJob(search, [], SearchExecutionState.empty(), []);
      const { result } = await executeJobResult({ jobIds });

      const errors = result.errors ?? [];

      // Errors without query attribution mean the whole execution failed.
      if (errors.length > 0 && errors.every((e) => !e.queryId)) {
        throw new Error(errors[0].description ?? 'Search failed');
      }

      // A query-level error (no searchTypeId) applies to every pane of that query; a search-type-scoped
      // error applies only to the pane backed by that search type. Without this, a failure in the
      // aggregation search type would otherwise be misattributed to the healthy messages pane, since
      // both search types now live on the same (source) query.
      const errorForSearchType = (queryId: string, searchTypeId: string) =>
        errors.find((e) => e.queryId === queryId && (!e.searchTypeId || e.searchTypeId === searchTypeId))?.description;

      return {
        selfLogs: toPreview(
          result.forId(ids.selfQueryId)?.searchTypes?.[ids.selfSearchTypeId] as RawMessagesResult | undefined,
        ),
        sourceLogs: toPreview(
          result.forId(ids.sourceQueryId)?.searchTypes?.[ids.sourceSearchTypeId] as RawMessagesResult | undefined,
        ),
        sourceCounts: toSourceCounts(
          result.forId(ids.sourceQueryId)?.searchTypes?.[ids.sourceCountsSearchTypeId] as RawPivotResult | undefined,
        ),
        selfLogsError: errorForSearchType(ids.selfQueryId, ids.selfSearchTypeId),
        sourceLogsError: errorForSearchType(ids.sourceQueryId, ids.sourceSearchTypeId),
      };
    },
  });

  const generalError = (createError ?? executeError ?? null) as Error | null;
  const paneError = (description: string | undefined) => generalError ?? (description ? new Error(description) : null);

  return {
    selfLogs: results?.selfLogs,
    sourceLogs: results?.sourceLogs,
    sourceCounts: results?.sourceCounts,
    selfLogsError: paneError(results?.selfLogsError),
    sourceLogsError: paneError(results?.sourceLogsError),
    isLoading: !createError && (!created || isLoading),
  };
};

export default useCollectorLogPreview;
