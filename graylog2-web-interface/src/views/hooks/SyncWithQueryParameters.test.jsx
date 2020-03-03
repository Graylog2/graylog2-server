// @flow strict
import history from 'util/History';

import asMock from 'helpers/mocking/AsMock';
import { ViewStore } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import { syncWithQueryParameters } from './SyncWithQueryParameters';
import Query, { createElasticsearchQueryString } from '../logic/queries/Query';
import Search from '../logic/search/Search';

jest.mock('views/stores/QueryFiltersStore', () => ({
  QueryFiltersActions: {
    streams: {
      completed: {
        listen: jest.fn(),
      },
    },
  },
}));

jest.mock('views/stores/ViewStore', () => ({
  ViewStore: {
    getInitialState: jest.fn(),
  },
}));
jest.mock('util/History', () => ({ push: jest.fn(), replace: jest.fn() }));

const createSearch = (timerange, queryString = 'foo:42') => Search.builder()
  .queries([
    Query.builder()
      .timerange(timerange)
      .query(createElasticsearchQueryString(queryString))
      .build(),
  ])
  .build();

const createView = (search, type = View.Type.Search) => View.builder()
  .type(type)
  .search(search)
  .build();

describe('SyncWithQueryParameters', () => {
  it('does not do anything if no view is loaded', () => {
    syncWithQueryParameters('');
    expect(history.push).not.toHaveBeenCalled();
  });
  it('does not do anything if current view is not a search', () => {
    asMock(ViewStore.getInitialState).mockReturnValueOnce({
      view: View.builder().type(View.Type.Dashboard).build(),
    });
    syncWithQueryParameters('');
    expect(history.push).not.toHaveBeenCalled();
  });
  describe('if current view is search, adds state to history', () => {
    const view = createView(createSearch({ type: 'relative', range: 600 }));
    it('with current time range and query', () => {
      asMock(ViewStore.getInitialState).mockReturnValueOnce({ view });

      syncWithQueryParameters('/search');

      expect(history.push).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&relative=600');
    });
    it('preserving query parameters present before', () => {
      asMock(ViewStore.getInitialState).mockReturnValueOnce({ view });

      syncWithQueryParameters('/search?somevalue=23&somethingelse=foo');

      expect(history.push).toHaveBeenCalledWith('/search?somevalue=23&somethingelse=foo&q=foo%3A42&rangetype=relative&relative=600');
    });
    it('if time range is absolute', () => {
      const viewWithAbsoluteTimerange = createView(createSearch({
        type: 'absolute',
        from: '2019-01-12T13:42:23.000Z',
        to: '2020-01-12T13:42:23.000Z',
      }));
      asMock(ViewStore.getInitialState).mockReturnValueOnce({ view: viewWithAbsoluteTimerange });

      syncWithQueryParameters('/search');

      expect(history.push)
        .toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=absolute&from=2019-01-12T13%3A42%3A23.000Z&to=2020-01-12T13%3A42%3A23.000Z');
    });
    it('if time range is keyword time range', () => {
      const viewWithAbsoluteTimerange = createView(createSearch({
        type: 'keyword',
        keyword: 'Last five minutes',
      }));
      asMock(ViewStore.getInitialState).mockReturnValueOnce({ view: viewWithAbsoluteTimerange });

      syncWithQueryParameters('/search');

      expect(history.push)
        .toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=keyword&keyword=Last+five+minutes');
    });
    it('by calling the provided action', () => {
      asMock(ViewStore.getInitialState).mockReturnValueOnce({ view });

      syncWithQueryParameters('/search', history.replace);

      expect(history.replace).toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=relative&relative=600');
    });
  });
});
