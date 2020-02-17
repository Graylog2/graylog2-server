// @flow strict
import history from 'util/History';

import asMock from 'helpers/mocking/AsMock';
import { ViewStore } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import { syncWithQueryParameters } from './SyncWithQueryParameters';
import Query, { createElasticsearchQueryString } from '../logic/queries/Query';
import Search from '../logic/search/Search';

jest.mock('views/stores/ViewStore', () => ({
  ViewStore: {
    getInitialState: jest.fn(),
  },
}));
jest.mock('util/History', () => ({ push: jest.fn(), replace: jest.fn() }));

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
    const view = View.builder()
      .type(View.Type.Search)
      .search(
        Search.builder()
          .queries([
            Query.builder()
              .timerange({ type: 'relative', range: 600 })
              .query(createElasticsearchQueryString('foo:42'))
              .build(),
          ])
          .build(),
      )
      .build();
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
      const viewWithAbsoluteTimerange = View.builder()
        .type(View.Type.Search)
        .search(
          Search.builder()
            .queries([
              Query.builder()
                .timerange({
                  type: 'absolute',
                  from: '2019-01-12T13:42:23.000Z',
                  to: '2020-01-12T13:42:23.000Z',
                })
                .query(createElasticsearchQueryString('foo:42'))
                .build(),
            ])
            .build(),
        )
        .build();
      asMock(ViewStore.getInitialState).mockReturnValueOnce({ view: viewWithAbsoluteTimerange });

      syncWithQueryParameters('/search');

      expect(history.push)
        .toHaveBeenCalledWith('/search?q=foo%3A42&rangetype=absolute&from=2019-01-12T13%3A42%3A23.000Z&to=2020-01-12T13%3A42%3A23.000Z');
    });
    it('if time range is keyword time range', () => {
      const viewWithAbsoluteTimerange = View.builder()
        .type(View.Type.Search)
        .search(
          Search.builder()
            .queries([
              Query.builder()
                .timerange({
                  type: 'keyword',
                  keyword: 'Last five minutes',
                })
                .query(createElasticsearchQueryString('foo:42'))
                .build(),
            ])
            .build(),
        )
        .build();
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
