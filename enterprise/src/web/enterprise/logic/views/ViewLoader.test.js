// @flow strict
import * as Immutable from 'immutable';

import { ViewManagementActions } from 'enterprise/stores/ViewManagementStore';
import { SearchActions } from 'enterprise/stores/SearchStore';
import { ViewActions } from 'enterprise/stores/ViewStore';
import Search from '../search/Search';
import DashboardState from './DashboardState';
import View from './View';
import ViewLoader from './ViewLoader';
import ViewState from './ViewState';

jest.mock('enterprise/stores/ViewManagementStore', () => ({
  ViewManagementActions: {},
}));
jest.mock('enterprise/stores/SearchStore', () => ({
  SearchActions: {},
}));
jest.mock('enterprise/stores/ViewStore', () => ({
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
          .build(),
      );
    });
  });
});
