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
import { act, renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import createSearch from 'views/logic/slices/createSearch';
import { startJob, executeJobResult } from 'views/logic/slices/executeJobResult';
import type Search from 'views/logic/search/Search';
import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import type Query from 'views/logic/queries/Query';
import type { AggregationSearchType } from 'views/logic/queries/SearchType';

import { useCollectorLogPreview } from './useCollectorLogPreview';

jest.mock('views/logic/slices/createSearch');
jest.mock('views/logic/slices/executeJobResult', () => ({
  startJob: jest.fn(),
  executeJobResult: jest.fn(),
}));

const resultMessage = (id: string, timestamp: string, text: string) => ({
  index: 'graylog_0',
  highlight_ranges: {},
  message: { _id: id, timestamp, message: text },
});

describe('useCollectorLogPreview', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(createSearch).mockImplementation(async (search: Search) => search);
    asMock(startJob).mockResolvedValue({ asyncSearchId: 'job-1', nodeId: 'node-1' });
  });

  const asExecutionResult = (result: unknown) => result as Awaited<ReturnType<typeof executeJobResult>>;

  const splitQueries = (search: Search) => {
    const queries = search.queries.toArray();

    return {
      sourceQuery: queries.find((q) => q.query.query_string.includes('NOT agent_receiver_type')),
      selfQuery: queries.find((q) => !q.query.query_string.includes('NOT agent_receiver_type')),
    };
  };

  const searchTypeIdByType = (query: Query, type: string) => query.searchTypes.find((st) => st.type === type).id;

  // Lets executeJobResult mocks reference the search that createSearch received.
  const captureCreatedSearch = () => {
    let createdSearch: Search;

    asMock(createSearch).mockImplementation(async (search: Search) => {
      createdSearch = search;

      return search;
    });

    return () => createdSearch;
  };

  const mockEmptyResults = () => {
    asMock(executeJobResult).mockResolvedValue(
      asExecutionResult({
        result: {
          errors: [],
          forId: () => undefined,
        },
      }),
    );
  };

  const makeResultsMock = (createdSearch: Search) => {
    const { sourceQuery, selfQuery } = splitQueries(createdSearch);

    return {
      result: {
        errors: [],
        forId: (queryId: string) => {
          if (queryId === sourceQuery.id) {
            return {
              searchTypes: {
                [searchTypeIdByType(sourceQuery, 'messages')]: {
                  type: 'messages',
                  messages: [resultMessage('m1', '2026-06-10T12:00:00.000Z', 'a source log line')],
                  total: 42,
                },
                [searchTypeIdByType(sourceQuery, 'pivot')]: {
                  type: 'pivot',
                  total: 42,
                  rows: [
                    {
                      source: 'leaf',
                      key: ['s1'],
                      values: [{ source: 'row-leaf', key: ['count()'], value: 40, rollup: false }],
                    },
                    {
                      source: 'leaf',
                      key: ['s2'],
                      values: [{ source: 'row-leaf', key: ['count()'], value: 2, rollup: false }],
                    },
                  ],
                },
              },
            };
          }

          if (queryId === selfQuery.id) {
            return {
              searchTypes: {
                [searchTypeIdByType(selfQuery, 'messages')]: {
                  type: 'messages',
                  messages: [resultMessage('m2', '2026-06-10T11:59:00.000Z', 'collector started')],
                  total: 7,
                },
              },
            };
          }

          return undefined;
        },
      },
    };
  };

  it('creates the search once and maps both result sets', async () => {
    const getCreatedSearch = captureCreatedSearch();

    asMock(executeJobResult).mockImplementation(async () => asExecutionResult(makeResultsMock(getCreatedSearch())));

    const { result } = renderHook(() => useCollectorLogPreview('uid-42'));

    await waitFor(() => expect(result.current.sourceLogs).toBeDefined());

    expect(createSearch).toHaveBeenCalledTimes(1);

    expect(result.current.sourceLogs).toEqual({
      messages: [{ id: 'm1', timestamp: '2026-06-10T12:00:00.000Z', text: 'a source log line' }],
      total: 42,
    });

    expect(result.current.selfLogs).toEqual({
      messages: [{ id: 'm2', timestamp: '2026-06-10T11:59:00.000Z', text: 'collector started' }],
      total: 7,
    });
  });

  it('builds the right queries: stream-scoped self-logs, source logs excluding self-logs', async () => {
    mockEmptyResults();

    renderHook(() => useCollectorLogPreview('uid-42'));

    await waitFor(() => expect(createSearch).toHaveBeenCalledTimes(1));

    const search: Search = asMock(createSearch).mock.calls[0][0];
    const queries = search.queries.toArray();

    expect(queries).toHaveLength(2);

    const queryStrings = queries.map((q) => q.query.query_string);

    expect(queryStrings).toContain('agent_id:"uid-42"');
    expect(queryStrings).toContain('agent_id:"uid-42" AND NOT agent_receiver_type:"collector_log"');

    const selfQuery = queries.find((q) => q.query.query_string === 'agent_id:"uid-42"');

    expect(selfQuery.filter.toJS()).toEqual({
      type: 'or',
      filters: [{ type: 'stream', id: '000000000000000000000005' }],
    });

    const sourceQuery = queries.find((q) => q.query.query_string.includes('NOT'));

    expect(sourceQuery.filter).toBeUndefined();

    queries.forEach((q) => {
      expect(q.timerange).toEqual({ type: 'relative', from: 3600 });

      const messagesSearchType = q.searchTypes.find((st) => st.type === 'messages');

      expect(messagesSearchType).toEqual(
        expect.objectContaining({
          type: 'messages',
          limit: 10,
          offset: 0,
          sort: [new MessageSortConfig('timestamp', Direction.Descending)],
        }),
      );
    });

    // The self-query only ever needs message previews; the source query also carries the
    // per-source aggregation used to compute sourceCounts.
    expect(selfQuery.searchTypes).toHaveLength(1);
    expect(sourceQuery.searchTypes).toHaveLength(2);
  });

  it('aggregates message counts per source', async () => {
    const getCreatedSearch = captureCreatedSearch();

    asMock(executeJobResult).mockImplementation(async () => asExecutionResult(makeResultsMock(getCreatedSearch())));

    const { result } = renderHook(() => useCollectorLogPreview('uid-42'));

    await waitFor(() => expect(result.current.sourceCounts).toBeDefined());

    expect(result.current.sourceCounts).toEqual({ s1: 40, s2: 2 });
  });

  it('groups the source counts by source id with an explicit bucket limit', async () => {
    mockEmptyResults();

    renderHook(() => useCollectorLogPreview('uid-42'));

    await waitFor(() => expect(createSearch).toHaveBeenCalledTimes(1));

    const search: Search = asMock(createSearch).mock.calls[0][0];
    const { sourceQuery } = splitQueries(search);
    // Query.searchTypes is a plain Array<SearchType>; the union's `type` field isn't a narrowing
    // discriminant (it's typed as `string`), so the aggregation-only fields need this assertion.
    const aggregation = sourceQuery.searchTypes.find((st) => st.type === 'pivot') as AggregationSearchType | undefined;

    // The wire discriminator must be `pivot` (`Pivot.NAME` server-side), not the frontend's
    // `PluggableSearchType` key `aggregation`. Sending `aggregation` deserialises to
    // `SearchType.Fallback`, whose null `filters` makes the search filter normalizer throw and fails
    // the entire search, so this assertion is load-bearing rather than cosmetic.
    expect(aggregation.type).toBe('pivot');
    expect(aggregation.row_groups).toEqual([{ type: 'values', fields: ['agent_source_id'], limit: 100 }]);
    // A `count` series must carry no field: count(<field>) counts occurrences of that field.
    expect(aggregation.series).toEqual([{ id: 'count()', type: 'count' }]);
  });

  it('reports unknown source counts as undefined rather than as all-zero', async () => {
    mockEmptyResults();

    const { result } = renderHook(() => useCollectorLogPreview('uid-42'));

    await waitFor(() => expect(result.current.sourceLogs).toBeDefined());

    expect(result.current.sourceCounts).toBeUndefined();
  });

  it('reports an empty aggregation as an empty object, distinct from an unavailable one', async () => {
    const getCreatedSearch = captureCreatedSearch();

    asMock(executeJobResult).mockImplementation(async () => {
      const { sourceQuery, selfQuery } = splitQueries(getCreatedSearch());

      return asExecutionResult({
        result: {
          errors: [],
          forId: (queryId: string) => {
            if (queryId === sourceQuery.id) {
              return {
                searchTypes: {
                  [searchTypeIdByType(sourceQuery, 'messages')]: {
                    type: 'messages',
                    messages: [],
                    total: 0,
                  },
                  [searchTypeIdByType(sourceQuery, 'pivot')]: {
                    type: 'pivot',
                    total: 0,
                    rows: [],
                  },
                },
              };
            }

            if (queryId === selfQuery.id) {
              return { searchTypes: {} };
            }

            return undefined;
          },
        },
      });
    });

    const { result } = renderHook(() => useCollectorLogPreview('uid-42'));

    await waitFor(() => expect(result.current.sourceLogs).toBeDefined());

    // The aggregation ran and returned no rows: every source is confirmed silent, which must stay
    // distinguishable from "the aggregation didn't run at all" (`undefined`).
    expect(result.current.sourceCounts).toEqual({});
    expect(result.current.sourceCounts).not.toBeUndefined();
  });

  it('executes the created search with the execution helpers', async () => {
    mockEmptyResults();

    renderHook(() => useCollectorLogPreview('uid-42'));

    await waitFor(() => expect(startJob).toHaveBeenCalled());
    await waitFor(() => expect(executeJobResult).toHaveBeenCalled());

    expect(executeJobResult).toHaveBeenCalledWith({
      jobIds: { asyncSearchId: 'job-1', nodeId: 'node-1' },
    });
  });

  it('surfaces search errors', async () => {
    asMock(executeJobResult).mockResolvedValue(
      asExecutionResult({
        result: {
          errors: [{ description: 'boom' }],
          forId: () => undefined,
        },
      }),
    );

    const { result } = renderHook(() => useCollectorLogPreview('uid-42'), {
      queryClientOptions: { defaultOptions: { queries: { retry: false } } },
    });

    await waitFor(() => expect(result.current.sourceLogsError).not.toBeNull());

    expect(result.current.sourceLogsError.message).toBe('boom');
    expect(result.current.selfLogsError).not.toBeNull();
    expect(result.current.selfLogsError.message).toBe('boom');
  });

  it('re-executes without recreating the search on the refresh interval', async () => {
    jest.useFakeTimers();

    try {
      mockEmptyResults();

      renderHook(() => useCollectorLogPreview('uid-42'));

      await waitFor(() => expect(startJob).toHaveBeenCalledTimes(1));

      await act(async () => {
        jest.advanceTimersByTime(5000);
      });

      await waitFor(() => expect(startJob).toHaveBeenCalledTimes(2));

      expect(createSearch).toHaveBeenCalledTimes(1);
    } finally {
      jest.useRealTimers();
    }
  });

  it('keeps last good data when a later execution fails', async () => {
    jest.useFakeTimers();

    try {
      const getCreatedSearch = captureCreatedSearch();

      asMock(executeJobResult).mockImplementation(async () => asExecutionResult(makeResultsMock(getCreatedSearch())));

      const { result } = renderHook(() => useCollectorLogPreview('uid-42'), {
        queryClientOptions: { defaultOptions: { queries: { retry: false } } },
      });

      await waitFor(() => expect(result.current.sourceLogs).toBeDefined());

      expect(result.current.sourceLogs.messages).toHaveLength(1);

      asMock(executeJobResult).mockRejectedValue(new Error('tick failed'));

      await act(async () => {
        jest.advanceTimersByTime(5000);
      });

      await waitFor(() => expect(result.current.sourceLogsError).not.toBeNull());

      expect(result.current.sourceLogs.messages).toHaveLength(1);
      expect(result.current.sourceLogsError).not.toBeNull();
    } finally {
      jest.useRealTimers();
    }
  });

  it('per-query error keeps the healthy pane', async () => {
    const getCreatedSearch = captureCreatedSearch();

    asMock(executeJobResult).mockImplementation(async () => {
      const { sourceQuery, selfQuery } = splitQueries(getCreatedSearch());

      return asExecutionResult({
        result: {
          errors: [{ queryId: selfQuery.id, description: 'denied' }],
          forId: (queryId: string) => {
            if (queryId === sourceQuery.id) {
              return {
                searchTypes: {
                  [sourceQuery.searchTypes[0].id]: {
                    type: 'messages',
                    messages: [resultMessage('m1', '2026-06-10T12:00:00.000Z', 'a source log line')],
                    total: 42,
                  },
                },
              };
            }

            return undefined;
          },
        },
      });
    });

    const { result } = renderHook(() => useCollectorLogPreview('uid-42'));

    await waitFor(() => expect(result.current.sourceLogs).toBeDefined());

    expect(result.current.selfLogsError.message).toBe('denied');
    expect(result.current.sourceLogs.messages).toHaveLength(1);
    expect(result.current.sourceLogsError).toBeNull();
  });

  it('an aggregation-scoped error does not poison the healthy messages pane on the same query', async () => {
    const getCreatedSearch = captureCreatedSearch();

    asMock(executeJobResult).mockImplementation(async () => {
      const { sourceQuery, selfQuery } = splitQueries(getCreatedSearch());
      const aggregationSearchTypeId = searchTypeIdByType(sourceQuery, 'pivot');

      return asExecutionResult({
        result: {
          errors: [{ queryId: sourceQuery.id, searchTypeId: aggregationSearchTypeId, description: 'agg failed' }],
          forId: (queryId: string) => {
            if (queryId === sourceQuery.id) {
              return {
                searchTypes: {
                  [searchTypeIdByType(sourceQuery, 'messages')]: {
                    type: 'messages',
                    messages: [resultMessage('m1', '2026-06-10T12:00:00.000Z', 'a source log line')],
                    total: 42,
                  },
                },
              };
            }

            if (queryId === selfQuery.id) {
              return { searchTypes: {} };
            }

            return undefined;
          },
        },
      });
    });

    const { result } = renderHook(() => useCollectorLogPreview('uid-42'));

    await waitFor(() => expect(result.current.sourceLogs).toBeDefined());

    // The failure belongs to the aggregation search type on the source query, not to the messages
    // search type that shares that query, so the messages pane must stay healthy.
    expect(result.current.sourceLogsError).toBeNull();
    expect(result.current.sourceLogs.messages).toHaveLength(1);
    expect(result.current.sourceLogs.messages[0].text).toBe('a source log line');
  });
});
