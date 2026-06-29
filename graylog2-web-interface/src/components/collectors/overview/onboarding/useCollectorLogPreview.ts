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
import type { MessagesSearchType } from 'views/logic/queries/SearchType';

// Deliberately outside the 'collectors' prefix: useCollectorsMutations invalidates
// ['collectors'] wholesale on every mutation, which would re-create the backend search.
export const ONBOARDING_KEY_PREFIX = ['collector-onboarding'];

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
  const paneError = (description: string | undefined) => generalError ?? (description ? new Error(description) : null);

  return {
    selfLogs: results?.selfLogs,
    sourceLogs: results?.sourceLogs,
    selfLogsError: paneError(results?.selfLogsError),
    sourceLogsError: paneError(results?.sourceLogsError),
    isLoading: !createError && (!created || isLoading),
  };
};

export default useCollectorLogPreview;
