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
import * as Immutable from 'immutable';

import View from 'views/logic/views/View';
import Query, { createElasticsearchQueryString, filtersForQuery } from 'views/logic/queries/Query';
import type { TimeRange, RelativeTimeRange } from 'views/logic/queries/Query';

import { syncWithQueryParameters } from './SyncWithQueryParameters';

jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/hooks/useViewType');

const lastFiveMinutes: RelativeTimeRange = { type: 'relative', from: 300 };
const createQuery = (timerange: TimeRange = lastFiveMinutes, streams: Array<string> = [], queryString = 'foo:42') => Query.builder()
  .timerange(timerange)
  .filter(filtersForQuery(streams) || Immutable.Map())
  .query(createElasticsearchQueryString(queryString))
  .build();

describe('SyncWithQueryParameters', () => {
  afterEach(() => { jest.clearAllMocks(); });

  it('does not do anything if no view is loaded', () => {
    const push = jest.fn();
    syncWithQueryParameters(View.Type.Search, '', undefined, push);

    expect(push).not.toHaveBeenCalled();
  });

  it('does not do anything if current view is not a search', () => {
    const push = jest.fn();
    syncWithQueryParameters(View.Type.Dashboard, '', undefined, push);

    expect(push).not.toHaveBeenCalled();
  });

  describe('if current view is search, adds state to history', () => {
    it('with current time range and query', () => {
      const push = jest.fn();
      syncWithQueryParameters(View.Type.Search, '/search', createQuery({ type: 'relative', range: 600 }), push);

      expect(push).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&relative=600');
    });

    it('preserving query parameters present before', () => {
      const push = jest.fn();
      syncWithQueryParameters(View.Type.Search, '/search?somevalue=23&somethingelse=foo', createQuery({ type: 'relative', range: 600 }), push);

      expect(push).toHaveBeenCalledWith('/search?somevalue=23&somethingelse=foo&q=foo%3A42&rangetype=relative&relative=600');
    });

    it('if time range is relative with from and to', () => {
      const push = jest.fn();
      syncWithQueryParameters(View.Type.Search, '/search', createQuery({ ...lastFiveMinutes, to: 240 }), push);

      expect(push).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300&to=240');
    });

    it('if time range is relative with from only', () => {
      const push = jest.fn();
      syncWithQueryParameters(View.Type.Search, '/search', createQuery(lastFiveMinutes), push);

      expect(push).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300');
    });

    it('if time range is absolute', () => {
      const push = jest.fn();

      syncWithQueryParameters(View.Type.Search, '/search', createQuery({
        type: 'absolute',
        from: '2019-01-12T13:42:23.000Z',
        to: '2020-01-12T13:42:23.000Z',
      }), push);

      expect(push)
        .toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=absolute&from=2019-01-12T13%3A42%3A23.000Z&to=2020-01-12T13%3A42%3A23.000Z');
    });

    it('if time range is keyword time range', () => {
      const push = jest.fn();

      syncWithQueryParameters(View.Type.Search, '/search', createQuery({
        type: 'keyword',
        keyword: 'Last five minutes',
      }), push);

      expect(push)
        .toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=keyword&keyword=Last+five+minutes');
    });

    it('by calling the provided action', () => {
      const replace = jest.fn();
      syncWithQueryParameters(View.Type.Search, '/search', createQuery({ type: 'relative', range: 600 }), replace);

      expect(replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&relative=600');
    });

    it('adds list of streams to query', () => {
      const push = jest.fn();
      syncWithQueryParameters(View.Type.Search, '/search', createQuery(lastFiveMinutes, ['stream1', 'stream2']), push);

      expect(push)
        .toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300&streams=stream1%2Cstream2');
    });

    it('removes list of streams to query if they become empty', () => {
      const push = jest.fn();
      syncWithQueryParameters(View.Type.Search, '/search?q=foo%3A42&rangetype=relative&from=300&streams=stream1%2Cstream2', createQuery(lastFiveMinutes), push);

      expect(push)
        .toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300');
    });
  });
});
