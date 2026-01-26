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
import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import View from 'views/logic/views/View';
import Query, { createElasticsearchQueryString, filtersForQuery } from 'views/logic/queries/Query';
import type { TimeRange, RelativeTimeRange } from 'views/logic/queries/Query';
import { asMock } from 'helpers/mocking';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useViewType from 'views/hooks/useViewType';
import useHistory from 'routing/useHistory';
import mockHistory from 'helpers/mocking/mockHistory';

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
  let history;

  beforeEach(() => {
    history = mockHistory();
    asMock(useHistory).mockReturnValue(history);
    asMock(useViewType).mockReturnValue(View.Type.Search);
    asMock(useCurrentQuery).mockReturnValue(createQuery(lastFiveMinutes));
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('does not do anything if no view is loaded', () => {
    asMock(useViewType).mockReturnValue(undefined);
    renderHook(() => useSyncWithQueryParameters('/search'));

    expect(history.replace).not.toHaveBeenCalled();
    expect(history.push).not.toHaveBeenCalled();
  });

  it('does not do anything if current view is not a search', () => {
    asMock(useViewType).mockReturnValue(View.Type.Dashboard);
    renderHook(() => useSyncWithQueryParameters('/search'));

    expect(history.replace).not.toHaveBeenCalled();
    expect(history.push).not.toHaveBeenCalled();
  });

  describe('if current view is search, adds state to history', () => {
    it('with current time range and query', () => {
      asMock(useCurrentQuery).mockReturnValue(createQuery({ type: 'relative', range: 600 }));
      renderHook(() => useSyncWithQueryParameters('/search'));

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&relative=600');
    });

    it('preserving query parameters present before', () => {
      asMock(useCurrentQuery).mockReturnValue(createQuery({ type: 'relative', range: 600 }));
      renderHook(() => useSyncWithQueryParameters('/search?somevalue=23&somethingelse=foo'));

      expect(history.replace).toHaveBeenCalledWith(
        '/search?somevalue=23&somethingelse=foo&q=foo%3A42&rangetype=relative&relative=600',
      );
    });

    it('if time range is relative with from and to', () => {
      asMock(useCurrentQuery).mockReturnValue(createQuery({ ...lastFiveMinutes, to: 240 }));
      renderHook(() => useSyncWithQueryParameters('/search'));

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300&to=240');
    });

    it('if time range is relative with from only', () => {
      asMock(useCurrentQuery).mockReturnValue(createQuery(lastFiveMinutes));
      renderHook(() => useSyncWithQueryParameters('/search'));

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300');
    });

    it('if time range is absolute', () => {
      asMock(useCurrentQuery).mockReturnValue(
        createQuery({
          type: 'absolute',
          from: '2019-01-12T13:42:23.000Z',
          to: '2020-01-12T13:42:23.000Z',
        }),
      );
      renderHook(() => useSyncWithQueryParameters('/search'));

      expect(history.replace).toHaveBeenCalledWith(
        '/search?q=foo%3A42&rangetype=absolute&from=2019-01-12T13%3A42%3A23.000Z&to=2020-01-12T13%3A42%3A23.000Z',
      );
    });

    it('if time range is keyword time range', () => {
      asMock(useCurrentQuery).mockReturnValue(
        createQuery({
          type: 'keyword',
          keyword: 'Last five minutes',
        }),
      );
      renderHook(() => useSyncWithQueryParameters('/search'));

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=keyword&keyword=Last+five+minutes');
    });

    it('adds list of streams to query', () => {
      asMock(useCurrentQuery).mockReturnValue(createQuery(lastFiveMinutes, ['stream1', 'stream2']));
      renderHook(() => useSyncWithQueryParameters('/search'));

      expect(history.replace).toHaveBeenCalledWith(
        '/search?q=foo%3A42&rangetype=relative&from=300&streams=stream1%2Cstream2',
      );
    });

    it('removes list of streams to query if they become empty', () => {
      renderHook(() =>
        useSyncWithQueryParameters('/search?q=foo%3A42&rangetype=relative&from=300&streams=stream1%2Cstream2'),
      );

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&from=300');
    });

    it('does not update history if URI is already correct for current view', () => {
      renderHook(() => useSyncWithQueryParameters('/search?rangetype=relative&from=300&q=foo%3A42'));

      expect(history.replace).not.toHaveBeenCalled();
      expect(history.push).not.toHaveBeenCalled();
    });

    it('does not update history if query parameters only differ in order', () => {
      const { rerender } = renderHook(({ uri }) => useSyncWithQueryParameters(uri), {
        initialProps: { uri: '/search?rangetype=relative&from=300&q=foo%3A42' },
      });

      rerender({ uri: '/search?q=foo%3A42&rangetype=relative&from=300' });

      expect(history.replace).not.toHaveBeenCalled();
      expect(history.push).not.toHaveBeenCalled();
    });

    it('uses push on updates after the initial sync', async () => {
      asMock(useCurrentQuery).mockReturnValue(createQuery({ type: 'relative', range: 300 }));

      const { rerender } = renderHook(({ uri }) => useSyncWithQueryParameters(uri), {
        initialProps: { uri: '/search' },
      });

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&relative=300');

      asMock(useCurrentQuery).mockReturnValue(createQuery({ type: 'relative', range: 900 }));

      rerender({ uri: '/search?q=foo%3A42&rangetype=relative&relative=300' });

      await waitFor(() =>
        expect(history.push).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&relative=900'),
      );
    });
  });
});
