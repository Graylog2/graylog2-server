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

import { COLLECTOR_SYSTEM_LOGS_STREAM_ID } from 'components/collectors/common/collectorSystemLogsUrl';
import generateId from 'logic/generateId';
import Query, { createElasticsearchQueryString, filtersForQuery } from 'views/logic/queries/Query';
import type { RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import createSearch from 'views/logic/slices/createSearch';
import { startJob, executeJobResult } from 'views/logic/slices/executeJobResult';

const REFRESH_INTERVAL_MS = 5000;
const PREVIEW_RANGE_SECONDS = 900; // last 15 minutes
const PREVIEW_MESSAGE_LIMIT = 10;

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
  };
};

const messagesSearchType = (id: string) => ({
  id,
  type: 'messages',
  limit: PREVIEW_MESSAGE_LIMIT,
  offset: 0,
  sort: [{ field: 'timestamp', order: 'DESC' }],
  decorators: [],
});

const previewTimerange: RelativeTimeRangeWithEnd = { type: 'relative', from: PREVIEW_RANGE_SECONDS };

const buildPreviewSearch = (instanceUid: string): PreviewSearch => {
  const ids = {
    selfQueryId: generateId(),
    selfSearchTypeId: generateId(),
    sourceQueryId: generateId(),
    sourceSearchTypeId: generateId(),
  };

  // Self-logs live in the dedicated (system-scoped) collector logs stream.
  const selfLogsQuery = Query.builder()
    .id(ids.selfQueryId)
    .query(createElasticsearchQueryString(`collector_instance_uid:"${instanceUid}"`))
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
        `collector_instance_uid:"${instanceUid}" AND NOT collector_receiver_type:"collector_log"`,
      ),
    )
    .timerange(previewTimerange)
    .searchTypes([messagesSearchType(ids.sourceSearchTypeId)])
    .build();

  const search = Search.builder().newId().queries([sourceLogsQuery, selfLogsQuery]).parameters([]).build();

  return { search, ids };
};

type RawResultMessage = { message: { _id: string; timestamp: string; message: unknown } };
type RawMessagesResult = { messages?: Array<RawResultMessage>; total?: number };

const toPreview = (searchTypeResult: RawMessagesResult | undefined): LogPreview => ({
  messages: (searchTypeResult?.messages ?? []).map((m) => ({
    id: m.message._id,
    timestamp: m.message.timestamp,
    text: String(m.message.message ?? ''),
  })),
  total: searchTypeResult?.total ?? 0,
});

const useCollectorLogPreview = (instanceUid: string) => {
  const { data: created, error: createError } = useQuery<PreviewSearch>({
    // Use a prefix outside 'collectors' to avoid being invalidated by useCollectorsMutations,
    // which calls queryClient.invalidateQueries({ queryKey: ['collectors'] }) on every mutation.
    queryKey: ['collector-onboarding', 'preview-search', instanceUid],
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
    queryKey: ['collector-onboarding', 'preview-results', created?.search?.id],
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

      const errorForQuery = (queryId: string) => errors.find((e) => e.queryId === queryId)?.description;

      return {
        selfLogs: toPreview(
          result.forId(ids.selfQueryId)?.searchTypes?.[ids.selfSearchTypeId] as RawMessagesResult | undefined,
        ),
        sourceLogs: toPreview(
          result.forId(ids.sourceQueryId)?.searchTypes?.[ids.sourceSearchTypeId] as RawMessagesResult | undefined,
        ),
        selfLogsError: errorForQuery(ids.selfQueryId),
        sourceLogsError: errorForQuery(ids.sourceQueryId),
      };
    },
  });

  const generalError = (createError ?? executeError ?? null) as Error | null;

  return {
    selfLogs: results?.selfLogs,
    sourceLogs: results?.sourceLogs,
    selfLogsError: generalError ?? (results?.selfLogsError ? new Error(results.selfLogsError) : null),
    sourceLogsError: generalError ?? (results?.sourceLogsError ? new Error(results.sourceLogsError) : null),
    isLoading: !createError && (!created || isLoading),
  };
};

export default useCollectorLogPreview;
