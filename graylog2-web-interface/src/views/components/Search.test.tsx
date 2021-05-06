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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
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
import { SearchExecutionResult } from 'views/actions/SearchActions';
import WindowLeaveMessage from 'views/components/common/WindowLeaveMessage';

import Search from './Search';
import DashboardSearchBar from './DashboardSearchBar';

import { useSyncWithQueryParameters } from '../hooks/SyncWithQueryParameters';

jest.mock('util/History');
jest.mock('components/layout/Footer', () => mockComponent('Footer'));

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
    setWidgetsToSearch: jest.fn(),
    executeWithCurrentState: jest.fn(),
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

jest.mock('views/components/QueryBar', () => mockComponent('QueryBar'));
jest.mock('views/components/SearchResult', () => mockComponent('SearchResult'));
jest.mock('views/stores/StreamsStore');
jest.mock('views/components/common/WindowLeaveMessage', () => jest.fn(mockComponent('WindowLeaveMessage')));
jest.mock('views/components/WithSearchStatus', () => (x) => x);
jest.mock('views/components/SearchBar', () => mockComponent('SearchBar'));

jest.mock('views/components/DashboardSearchBar', () => ({ onExecute }: { onExecute: (view: View) => Promise<void> }) => (
  <button type="button" onClick={() => onExecute({ search: {} } as View)}>Execute Query</button>
));

jest.mock('views/stores/SearchMetadataStore');
jest.mock('views/components/views/CurrentViewTypeProvider', () => jest.fn());
jest.mock('views/hooks/SyncWithQueryParameters');
jest.mock('routing/withLocation', () => (Component) => (props) => <Component location={{ query: {}, pathname: '', search: '' }} {...props} />);
jest.mock('views/components/contexts/WidgetFieldTypesContextProvider', () => ({ children }) => children);

const mockPromise = <T, >(res: T): Promise<T> => {
  const promise = Promise.resolve(res);

  // @ts-ignore
  promise.then = (x) => x(res);

  return promise;
};

describe('Search', () => {
  beforeEach(() => {
    WidgetStore.listen = jest.fn(() => jest.fn());
    QueryFiltersStore.listen = jest.fn(() => jest.fn());
    SearchActions.execute = mockAction(jest.fn(async () => ({} as SearchExecutionResult)));
    StreamsActions.refresh = mockAction(jest.fn());
    SearchConfigActions.refresh = mockAction(jest.fn());
    SearchExecutionStateStore.listen = jest.fn(() => jest.fn());
    ViewActions.search.completed.listen = jest.fn(() => jest.fn());

    ViewStore.getInitialState = jest.fn(() => ({
      view: View.create().toBuilder().type(View.Type.Dashboard).build(),
      dirty: false,
      isNew: true,
      activeQuery: 'foobar',
    }));

    FieldTypesActions.all = mockAction(jest.fn(async () => {}));
    FieldTypesActions.refresh = mockAction(jest.fn(async () => {}));
    SearchMetadataActions.parseSearch = mockAction(jest.fn(() => mockPromise(SearchMetadata.empty())));
    SearchMetadataStore.listen = jest.fn(() => jest.fn());
    SearchActions.refresh = mockAction(jest.fn(() => Promise.resolve()));
    asMock(CurrentViewTypeProvider as React.FunctionComponent).mockImplementation(({ children }) => <ViewTypeContext.Provider value={View.Type.Dashboard}>{children}</ViewTypeContext.Provider>);
  });

  const SimpleSearch = (props) => (
    <Search location={{ query: {}, pathname: '/search', search: '' }}
            searchRefreshHooks={[]}
            {...props} />
  );

  const newSearchCallback = async () => {
    await waitFor(() => expect(ViewActions.search.completed.listen).toHaveBeenCalled());

    return asMock(ViewActions.search.completed.listen).mock.calls[0][0];
  };

  it('register a WindowLeaveMessage', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(WindowLeaveMessage).toHaveBeenCalled());
  });

  it('executes search upon mount', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(SearchActions.execute).toHaveBeenCalled());
  });

  it('refreshes search config upon mount', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(SearchConfigActions.refresh).toHaveBeenCalled());
  });

  it('does not register to QueryFiltersStore upon mount', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(QueryFiltersStore.listen).not.toHaveBeenCalled());
  });

  it('does not unregister from Query Filter store upon unmount', () => {
    const unsubscribe = jest.fn();

    QueryFiltersStore.listen = jest.fn(() => unsubscribe);
    const { unmount } = render(<SimpleSearch />);

    unmount();

    expect(unsubscribe).not.toHaveBeenCalled();
  });

  it('registers to SearchActions.refresh upon mount', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(SearchActions.refresh.listen).toHaveBeenCalled());
  });

  it('registers to ViewActions.search.completed upon mount', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(ViewActions.search.completed.listen).toHaveBeenCalled());
  });

  it('registers to ViewActions.search.completed even if search refresh condition fails', async () => {
    render(<SimpleSearch searchRefreshHools={[() => false]} />);

    await waitFor(() => expect(ViewActions.search.completed.listen).toHaveBeenCalled());
  });

  it('unregisters from ViewActions.search.completed upon unmount', async () => {
    const unsubscribe = jest.fn();

    ViewActions.search.completed.listen = jest.fn(() => unsubscribe);
    const { unmount } = render(<SimpleSearch />);

    await waitFor(() => expect(ViewActions.search.completed.listen).toHaveBeenCalled());

    expect(unsubscribe).not.toHaveBeenCalled();

    unmount();

    await waitFor(() => expect(unsubscribe).toHaveBeenCalled());
  });

  it('refreshes Streams upon mount', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(StreamsActions.refresh).toHaveBeenCalled());
  });

  it('synchronizes URL upon mount', async () => {
    render(<SimpleSearch />);

    await waitFor(() => expect(useSyncWithQueryParameters).toHaveBeenCalled());
  });

  it('updating search in view triggers search execution', async () => {
    render(<SimpleSearch />);

    const cb = await newSearchCallback();

    asMock(SearchActions.execute).mockClear();

    expect(SearchActions.execute).not.toHaveBeenCalled();

    cb({ search: {} } as View);

    await waitFor(() => expect(SearchActions.execute).toHaveBeenCalled());
  });

  it('refreshes field types store upon mount', async () => {
    expect(FieldTypesActions.refresh).not.toHaveBeenCalled();

    render(<SimpleSearch />);

    await waitFor(() => expect(FieldTypesActions.refresh).toHaveBeenCalled());
  });

  it('refreshes field types upon every search execution', async () => {
    render(<SimpleSearch />);

    asMock(FieldTypesActions.refresh).mockClear();

    const cb = await newSearchCallback();
    cb({ search: {} } as View);

    await waitFor(() => expect(FieldTypesActions.refresh).toHaveBeenCalled());
  });

  it('refreshing after query change parses search metadata first', async () => {
    render(<SimpleSearch />);

    const executeQuery = await screen.findByRole('button', { name: 'Execute Query' });

    fireEvent.click(executeQuery);

    await waitFor(() => expect(SearchMetadataActions.parseSearch).toHaveBeenCalled());

    expect(SearchActions.execute).toHaveBeenCalled();
  });

  it('changing current query in view does not trigger search execution', async () => {
    render(<SimpleSearch />);

    asMock(SearchActions.execute).mockClear();

    expect(SearchActions.execute).not.toHaveBeenCalled();

    await ViewActions.selectQuery('someQuery');

    expect(SearchActions.execute).not.toHaveBeenCalled();
  });
});
