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
// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';
import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import mockComponent from 'helpers/mocking/MockComponent';
import mockAction from 'helpers/mocking/MockAction';

import { StreamsActions } from 'views/stores/StreamsStore';
import { WidgetStore } from 'views/stores/WidgetStore';
import { QueryFiltersStore } from 'views/stores/QueryFiltersStore';
import { SearchActions } from 'views/stores/SearchStore';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchConfigActions } from 'views/stores/SearchConfigStore';
import { ViewActions, ViewStore } from 'views/stores/ViewStore';
import { FieldTypesActions } from 'views/stores/FieldTypesStore';
import { SearchMetadataActions, SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import View from 'views/logic/views/View';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import CurrentViewTypeProvider from 'views/components/views/CurrentViewTypeProvider';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

import Search from './Search';

import { useSyncWithQueryParameters } from '../hooks/SyncWithQueryParameters';

jest.mock('components/layout/Footer', () => <div />);
jest.mock('util/History');

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', () => ({
      activeQuery: 'beef-dead',
    })],
  ),
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {
    execute: jest.fn(() => Promise.resolve()),
  },
  SearchStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', () => ({
      result: {
        forId: jest.fn(() => {
          return {};
        }),
      },
    })],
  ),
}));

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesActions: {},
  FieldTypesStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', () => ({
      all: {},
      queryFields: {
        get: jest.fn(() => {
          return {};
        }),
      },
    })],
  ),
}));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: {
    listen: () => jest.fn(),
    getInitialState: () => ({
      searchesClusterConfig: {},
    }),
  },
  SearchConfigActions: {},
}));

jest.mock('components/layout/Footer', () => mockComponent('Footer'));
jest.mock('views/components/QueryBar', () => mockComponent('QueryBar'));
jest.mock('views/components/SearchResult', () => mockComponent('SearchResult'));
jest.mock('views/stores/StreamsStore');
jest.mock('views/components/common/WindowLeaveMessage', () => mockComponent('WindowLeaveMessage'));
jest.mock('views/components/WithSearchStatus', () => (x) => x);
jest.mock('views/components/SearchBar', () => mockComponent('SearchBar'));
jest.mock('views/components/DashboardSearchBar', () => mockComponent('DashboardSearchBar'));
jest.mock('views/stores/SearchMetadataStore');
jest.mock('views/components/views/CurrentViewTypeProvider', () => jest.fn());
jest.mock('views/hooks/SyncWithQueryParameters');
jest.mock('routing/withLocation', () => (Component) => (props) => <Component location={{ query: {}, pathname: '', search: '' }} {...props} />);

const mockPromise = (res) => {
  const promise = Promise.resolve(res);

  // @ts-ignore
  promise.then = (x) => x(res);

  return promise;
};

describe('Search', () => {
  beforeEach(() => {
    WidgetStore.listen = jest.fn(() => jest.fn());
    QueryFiltersStore.listen = jest.fn(() => jest.fn());
    // @ts-ignore
    SearchActions.execute = jest.fn(() => mockPromise());
    StreamsActions.refresh = jest.fn();
    SearchConfigActions.refresh = jest.fn();
    SearchExecutionStateStore.listen = jest.fn(() => jest.fn());
    ViewActions.search.completed.listen = jest.fn(() => jest.fn());

    ViewStore.getInitialState = jest.fn(() => ({
      view: View.create().toBuilder().type(View.Type.Dashboard).build(),
      dirty: false,
      isNew: true,
      activeQuery: 'foobar',
    }));

    FieldTypesActions.all = mockAction(jest.fn());
    SearchMetadataActions.parseSearch = mockAction(jest.fn(() => mockPromise(SearchMetadata.empty())));
    SearchMetadataStore.listen = jest.fn(() => jest.fn());
    // $FlowFixMe: Somehow flow does not see the `listen` property.
    SearchActions.refresh = mockAction(jest.fn(() => Promise.resolve()));
    asMock(CurrentViewTypeProvider).mockImplementation(({ children }) => <ViewTypeContext.Provider value={View.Type.Dashboard}>{children}</ViewTypeContext.Provider>);
  });

  const SimpleSearch = (props) => (
    <Search location={{ query: {}, pathname: '/search', search: '' }}
            searchRefreshHooks={[]}
            {...props} />
  );

  it('register a WindowLeaveMessage', () => {
    const wrapper = mount(<SimpleSearch />);

    expect(wrapper.find('WindowLeaveMessage')).toHaveLength(1);
  });

  it('executes search upon mount', () => {
    mount(<SimpleSearch />);

    expect(SearchActions.execute).toHaveBeenCalled();
  });

  it('refreshes search config upon mount', () => {
    mount(<SimpleSearch />);

    expect(SearchConfigActions.refresh).toHaveBeenCalled();
  });

  it('does not register to WidgetStore upon mount', () => {
    mount(<SimpleSearch />);

    expect(WidgetStore.listen).not.toHaveBeenCalled();
  });

  it('does not unregister from Widget store upon unmount', () => {
    const unsubscribe = jest.fn();

    WidgetStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<SimpleSearch />);

    wrapper.unmount();

    expect(unsubscribe).not.toHaveBeenCalled();
  });

  it('does not register to QueryFiltersStore upon mount', () => {
    mount(<SimpleSearch />);

    expect(QueryFiltersStore.listen).not.toHaveBeenCalled();
  });

  it('does not unregister from Query Filter store upon unmount', () => {
    const unsubscribe = jest.fn();

    QueryFiltersStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<SimpleSearch />);

    wrapper.unmount();

    expect(unsubscribe).not.toHaveBeenCalled();
  });

  it('registers to SearchActions.refresh upon mount', () => {
    mount(<SimpleSearch />);

    expect(SearchActions.refresh.listen).toHaveBeenCalled();
  });

  it('registers to ViewActions.search.completed upon mount', () => {
    mount(<SimpleSearch />);

    expect(ViewActions.search.completed.listen).toHaveBeenCalled();
  });

  it('registers to ViewActions.search.completed even if search refresh condition fails', () => {
    mount(<SimpleSearch searchRefreshHools={[() => false]} />);

    expect(ViewActions.search.completed.listen).toHaveBeenCalled();
  });

  it('unregisters from ViewActions.search.completed upon unmount', () => {
    const unsubscribe = jest.fn();

    ViewActions.search.completed.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<SimpleSearch />);

    expect(unsubscribe).not.toHaveBeenCalled();

    wrapper.unmount();

    expect(unsubscribe).toHaveBeenCalled();
  });

  it('refreshes Streams upon mount', () => {
    mount(<SimpleSearch />);

    expect(StreamsActions.refresh).toHaveBeenCalled();
  });

  it('synchronizes URL upon mount', async () => {
    mount(<SimpleSearch />);

    expect(useSyncWithQueryParameters).toHaveBeenCalled();
  });

  it('updating search in view triggers search execution', () => {
    mount(<SimpleSearch />);

    const cb = asMock(ViewActions.search.completed.listen).mock.calls[0][0];

    asMock(SearchActions.execute).mockClear();

    expect(SearchActions.execute).not.toHaveBeenCalled();

    return cb({ search: {} })
      .then(() => {
        expect(SearchActions.execute).toHaveBeenCalled();
      });
  });

  it('refreshes field types store upon mount', () => {
    expect(FieldTypesActions.all).not.toHaveBeenCalled();

    mount(<SimpleSearch />);

    expect(FieldTypesActions.all).toHaveBeenCalled();
  });

  it('refreshes field types upon every search execution', () => {
    mount(<SimpleSearch />);

    asMock(FieldTypesActions.all).mockClear();
    const cb = asMock(ViewActions.search.completed.listen).mock.calls[0][0];

    return cb({ search: {} })
      .then(() => {
        expect(FieldTypesActions.all).toHaveBeenCalled();
      });
  });

  it('refreshing after query change parses search metadata first', (done) => {
    const wrapper = mount(<SimpleSearch />);

    const searchBar = wrapper.find('DashboardSearchBar');
    const cb = searchBar.at(0).props().onExecute;

    const view = { search: {} };

    const promise = cb(view);

    promise.then(() => {
      expect(SearchMetadataActions.parseSearch).toHaveBeenCalled();
      expect(SearchActions.execute).toHaveBeenCalled();

      done();
    });
  });

  it('changing current query in view does not trigger search execution', () => {
    mount(<SimpleSearch />);

    asMock(SearchActions.execute).mockClear();

    expect(SearchActions.execute).not.toHaveBeenCalled();

    return ViewActions.selectQuery('someQuery')
      .then(() => {
        expect(SearchActions.execute).not.toHaveBeenCalled();
      });
  });
});
