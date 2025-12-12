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
import { renderHook } from 'wrappedTestingLibrary/hooks';

import type { ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import Query, { createElasticsearchQueryString, filtersForQuery } from 'views/logic/queries/Query';
import type { TimeRange, RelativeTimeRange } from 'views/logic/queries/Query';
import { asMock } from 'helpers/mocking';
import mockHistory from 'helpers/mocking/mockHistory';
import useHistory from 'routing/useHistory';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useViewType from 'views/hooks/useViewType';

import useSyncWithQueryParameters from './useSyncWithQueryParameters';

jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/hooks/useViewType');
jest.mock('routing/useHistory');

const lastFiveMinutes: RelativeTimeRange = { type: 'relative', from: 300 };
const createQuery = (timerange: TimeRange = lastFiveMinutes, streams: Array<string> = [], queryString = 'foo:42') =>
  Query.builder()
    .timerange(timerange)
    .filter(filtersForQuery(streams) || Immutable.Map())
    .query(createElasticsearchQueryString(queryString))
    .build();

describe('SyncWithQueryParameters', () => {
  const syncWithQueryParameters = (
    currentUri: string,
    query: Query | undefined,
    viewType: ViewType = View.Type.Search,
  ) => {
    const history = mockHistory();

    asMock(useHistory).mockReturnValue(history);
    asMock(useViewType).mockReturnValue(viewType);
    asMock(useCurrentQuery).mockReturnValue(query);

    renderHook(() => useSyncWithQueryParameters(currentUri));

    return history;
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('does not do anything if no view is loaded', () => {
    const history = syncWithQueryParameters('/search', undefined);

    expect(history.replace).not.toHaveBeenCalled();
    expect(history.push).not.toHaveBeenCalled();
  });

  it('does not do anything if current view is not a search', () => {
    const history = syncWithQueryParameters('/search', createQuery(), View.Type.Dashboard);

    expect(history.replace).not.toHaveBeenCalled();
    expect(history.push).not.toHaveBeenCalled();
  });

  describe('if current view is search, adds state to history', () => {
    it('with current time range and query', () => {
      const history = syncWithQueryParameters('/search', createQuery({ type: 'relative', range: 600 }));

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&relative=600');
    });

    it('preserving query parameters present before', () => {
      const history = syncWithQueryParameters(
        '/search?somevalue=23&somethingelse=foo',
        createQuery({ type: 'relative', range: 600 }),
      );

      expect(history.replace).toHaveBeenCalledWith(
        '/search?somevalue=23&somethingelse=foo&q=foo%3A42&rangetype=relative&relative=600',
      );
    });

    it('if time range is relative with from and to', () => {
      const history = syncWithQueryParameters('/search', createQuery({ ...lastFiveMinutes, to: 240 }));

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300&to=240');
    });

    it('if time range is relative with from only', () => {
      const history = syncWithQueryParameters('/search', createQuery(lastFiveMinutes));

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300');
    });

    it('if time range is absolute', () => {
      const history = syncWithQueryParameters(
        '/search',
        createQuery({
          type: 'absolute',
          from: '2019-01-12T13:42:23.000Z',
          to: '2020-01-12T13:42:23.000Z',
        }),
      );

      expect(history.replace).toHaveBeenCalledWith(
        '/search?q=foo%3A42&rangetype=absolute&from=2019-01-12T13%3A42%3A23.000Z&to=2020-01-12T13%3A42%3A23.000Z',
      );
    });

    it('if time range is keyword time range', () => {
      const history = syncWithQueryParameters(
        '/search',
        createQuery({
          type: 'keyword',
          keyword: 'Last five minutes',
        }),
      );

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=keyword&keyword=Last+five+minutes');
    });

    it('adds list of streams to query', () => {
      const history = syncWithQueryParameters('/search', createQuery(lastFiveMinutes, ['stream1', 'stream2']));

      expect(history.replace).toHaveBeenCalledWith(
        '/search?q=foo%3A42&rangetype=relative&from=300&streams=stream1%2Cstream2',
      );
    });

    it('removes list of streams to query if they become empty', () => {
      const history = syncWithQueryParameters(
        '/search?q=foo%3A42&rangetype=relative&from=300&streams=stream1%2Cstream2',
        createQuery(lastFiveMinutes),
      );

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300');
    });

    it('does not update history if query parameters only differ in order', () => {
      const history = syncWithQueryParameters(
        '/search?rangetype=relative&from=300&q=foo%3A42',
        createQuery(lastFiveMinutes),
      );

      expect(history.replace).not.toHaveBeenCalled();
      expect(history.push).not.toHaveBeenCalled();
    });
  });
});
