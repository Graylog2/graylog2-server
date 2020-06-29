// @flow strict
import * as Immutable from 'immutable';
import mockAction from 'helpers/mocking/MockAction';

import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { SearchActions } from 'views/stores/SearchStore';
import { ViewActions } from 'views/stores/ViewStore';

import View from './View';
import ViewLoader from './ViewLoader';
import type { ViewJson } from './View';

import Search from '../search/Search';

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {},
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {},
}));

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {},
}));

const viewJson: ViewJson = {
  id: 'foo',
  type: 'SEARCH',
  title: 'Foo',
  summary: 'summary',
  description: 'Foo',
  search_id: 'foosearch',
  properties: Immutable.List(),
  state: {},
  created_at: new Date('2019-05-24T12:34:04.993Z'),
  owner: 'admin',
  requires: {},
};

describe('ViewLoader', () => {
  beforeEach(() => {
    ViewManagementActions.get = mockAction(jest.fn(() => Promise.resolve(viewJson)));
    // $FlowFixMe: Return type ignored in test.
    ViewActions.load = jest.fn(() => Promise.resolve());
  });

  it('deserializes a view', () => {
    SearchActions.get = mockAction(jest.fn((id) => Promise.resolve({ id, queries: [], parameters: [] })));

    return ViewLoader('foo').then((result) => {
      expect(ViewManagementActions.get).toHaveBeenCalledWith('foo');
      expect(result).toEqual(
        View.builder()
          .id('foo')
          .type('SEARCH')
          .title('Foo')
          .summary('summary')
          .description('Foo')
          .properties(Immutable.List())
          .state(Immutable.Map())
          .createdAt(new Date('2019-05-24T12:34:04.993Z'))
          .owner('admin')
          .search(
            Search.create()
              .toBuilder()
              .id('foosearch')
              .parameters([])
              .queries([])
              .build(),
          )
          .requires({})
          .build(),
      );
    });
  });
});
