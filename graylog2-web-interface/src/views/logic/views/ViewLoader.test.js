// @flow strict
import * as Immutable from 'immutable';

import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { SearchActions } from 'views/stores/SearchStore';
import { ViewActions } from 'views/stores/ViewStore';
import Search from '../search/Search';
import DashboardState from './DashboardState';
import View from './View';
import ViewLoader from './ViewLoader';

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {},
}));
jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {},
}));
jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {},
}));

const viewJson = {
  id: 'foo',
  title: 'Foo',
  summary: 'summary',
  description: 'Foo',
  search_id: 'foosearch',
  properties: {},
  state: {},
  dashboard_state: { widgets: [], positions: [] },
  created_at: new Date('2019-05-24T12:34:04.993Z'),
  owner: 'admin',
  requires: {},
};

describe('ViewLoader', () => {
  beforeEach(() => {
    ViewManagementActions.get = jest.fn(() => Promise.resolve(viewJson));
    // $FlowFixMe: Return type ignored in test.
    ViewActions.load = jest.fn(() => Promise.resolve());
  });
  it('deserializes a view', () => {
    SearchActions.get = jest.fn(id => Promise.resolve({ id, queries: [], parameters: [] }));
    return ViewLoader('foo').then((result) => {
      expect(ViewManagementActions.get).toHaveBeenCalledWith('foo');
      expect(result).toEqual(
        View.builder()
          .id('foo')
          .title('Foo')
          .summary('summary')
          .description('Foo')
          .properties({})
          .state(Immutable.Map())
          .dashboardState(DashboardState.create())
          .createdAt(new Date('2019-05-24T12:34:04.993Z'))
          .owner('admin')
          .search(
            Search.create()
              .toBuilder()
              .id('foosearch')
              .parameters(Immutable.Set())
              .queries(Immutable.OrderedSet())
              .build(),
          )
          .requires({})
          .build(),
      );
    });
  });
});
