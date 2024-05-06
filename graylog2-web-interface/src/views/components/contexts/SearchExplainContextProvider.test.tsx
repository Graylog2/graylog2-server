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
import { useContext } from 'react';
import * as Immutable from 'immutable';
import { defaultUser as mockDefaultUser } from 'defaultMockValues';
import { waitFor } from '@testing-library/react';
import { renderHook } from 'wrappedTestingLibrary/hooks';

import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import fetch from 'logic/rest/FetchProvider';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import SearchExplainContextProvider from 'views/components/contexts/SearchExplainContextProvider';
import SearchExplainContext from 'views/components/contexts/SearchExplainContext';

jest.mock('stores/system/SystemStore', () => ({ SystemStore: MockStore() }));

jest.mock('stores/sessions/SessionStore', () => ({
  SessionStore: MockStore(['isLoggedIn', jest.fn()]),
}));

jest.mock('stores/notifications/NotificationsStore', () => ({
  NotificationsActions: { list: (jest.fn()) },
  NotificationsStore: MockStore(),
}));

jest.mock('stores/users/CurrentUserStore', () => ({
  __esModule: true,
  CurrentUserStore: {
    listen: () => jest.fn(),
    getInitialState: () => ({ currentUser: mockDefaultUser.toJSON() }),
  },
}));

jest.mock('views/stores/StreamsStore', () => ({ StreamsStore: MockStore(['getInitialState', () => ({ streams: [] })]) }));

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

const explainedWidget = {
  query_string: "{\"from\":0,\"size\":0,\"query\":{\"bool\":{\"must\":[{\"bool\":{\"filter\":[{\"match_all\":{\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"range\":{\"timestamp\":{\"from\":\"2023-09-11 20:55:50.185\",\"to\":\"2024-01-18 14:49:10.185\",\"include_lower\":true,\"include_upper\":false,\"boost\":1.0}}},{\"terms\":{\"streams\":[\"63d6d52ebf9c684b3da2deb3\",\"63a5ab32e71520111ed3ce06\",\"000000000000000000000001\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"track_total_hits\":2147483647,\"aggregations\":{\"agg\":{\"filters\":{\"filters\":[{\"bool\":{\"should\":[{\"exists\":{\"field\":\"source\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}}],\"other_bucket\":true,\"other_bucket_key\":\"_other_\"},\"aggregations\":{\"agg\":{\"terms\":{\"script\":{\"source\":\"(doc.containsKey('source') && doc['source'].size() > 0\\n? doc['source'].size() > 1\\n    ? doc['source']\\n    : String.valueOf(doc['source'].value)\\n: \\\"(Empty Value)\\\")\\n\",\"lang\":\"painless\"},\"size\":10,\"min_doc_count\":1,\"shard_min_doc_count\":0,\"show_term_doc_count_error\":false,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]}}}},\"timestamp-min\":{\"min\":{\"field\":\"timestamp\"}},\"timestamp-max\":{\"max\":{\"field\":\"timestamp\"}}}}",
  searched_index_ranges: [
    {
      index_name: 'graylog_0',
      begin: 0,
      end: 0,
      is_warm_tiered: false,
    },
    {
      index_name: 'bar_1512',
      begin: 1705589036047,
      end: 1705589284808,
      is_warm_tiered: false,
    },
    {
      index_name: 'bar_1513',
      begin: 0,
      end: 0,
      is_warm_tiered: false,
    },
    {
      index_name: 'bar_warm_1511',
      begin: 1705588785906,
      end: 1705589035782,
      is_warm_tiered: true,
    },
  ],
};

const mockData = {
  search_id: '647f0565d060431199a12e96',
  search: {
    queries: {
      'a1647eb6-a064-4fe6-b459-1e4267d3f659': {
        search_types: {
          '22249f29-f042-4bd8-b745-252b00a35891': explainedWidget,
          '5e9a9bfe-7a97-4835-86fd-896f40b20531': {
            query_string: "{\"from\":0,\"size\":0,\"query\":{\"bool\":{\"must\":[{\"bool\":{\"filter\":[{\"match_all\":{\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"range\":{\"timestamp\":{\"from\":\"2023-09-11 20:55:50.185\",\"to\":\"2024-01-18 14:49:10.185\",\"include_lower\":true,\"include_upper\":false,\"boost\":1.0}}},{\"terms\":{\"streams\":[\"63d6d52ebf9c684b3da2deb3\",\"63a5ab32e71520111ed3ce06\",\"000000000000000000000001\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"track_total_hits\":2147483647,\"aggregations\":{\"agg\":{\"filters\":{\"filters\":[{\"bool\":{\"should\":[{\"exists\":{\"field\":\"source\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}}],\"other_bucket\":true,\"other_bucket_key\":\"_other_\"},\"aggregations\":{\"agg\":{\"terms\":{\"script\":{\"source\":\"(doc.containsKey('source') && doc['source'].size() > 0\\n? doc['source'].size() > 1\\n    ? doc['source']\\n    : String.valueOf(doc['source'].value)\\n: \\\"(Empty Value)\\\")\\n\",\"lang\":\"painless\"},\"size\":15,\"min_doc_count\":1,\"shard_min_doc_count\":0,\"show_term_doc_count_error\":false,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]}}}},\"timestamp-min\":{\"min\":{\"field\":\"timestamp\"}},\"timestamp-max\":{\"max\":{\"field\":\"timestamp\"}}}}",
            searched_index_ranges: [
              {
                index_name: 'graylog_0',
                begin: 0,
                end: 0,
                is_warm_tiered: false,
              },
            ],
          },
        },
      },
    },
  },
  search_errors: [
  ],
};

describe('SearchExplainContextProvider', () => {
  useViewsPlugin();

  afterEach(() => {
    jest.clearAllMocks();
  });

  const provider = ({ children }) => (
    <TestStoreProvider>
      <SearchExplainContextProvider>
        {children}
      </SearchExplainContextProvider>
    </TestStoreProvider>
  );

  it('fetches and sets search/explain', async () => {
    asMock(fetch).mockImplementation(() => Promise.resolve(mockData));

    const { result, rerender } = renderHook(() => useContext(SearchExplainContext), { wrapper: provider });

    rerender();

    await waitFor(() => expect(fetch).toHaveBeenCalled());

    expect(result.current.explainedSearch).toEqual(mockData);
  });

  describe('getExplainForWidget', () => {
    it('retrieves the result for a specific widget', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve(mockData));

      const { result, rerender } = renderHook(() => useContext(SearchExplainContext), { wrapper: provider });

      rerender();

      await waitFor(() => expect(fetch).toHaveBeenCalled());

      expect(result.current.getExplainForWidget(
        'a1647eb6-a064-4fe6-b459-1e4267d3f659',
        'foo',
        Immutable.Map({ foo: Immutable.Set(['22249f29-f042-4bd8-b745-252b00a35891']) }),
      )).toEqual(explainedWidget);
    });
  });
});
