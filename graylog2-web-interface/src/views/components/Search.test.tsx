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
import { SearchMetadataActions, SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import View from 'views/logic/views/View';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import CurrentViewTypeProvider from 'views/components/views/CurrentViewTypeProvider';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import type { SearchExecutionResult } from 'views/actions/SearchActions';
import WindowLeaveMessage from 'views/components/common/WindowLeaveMessage';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import Query, { filtersForQuery } from 'views/logic/queries/Query';
import usePluginEntities from 'views/logic/usePluginEntities';

import Search from './Search';

import { useSyncWithQueryParameters } from '../hooks/SyncWithQueryParameters';

jest.mock('util/History');

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(
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

const mockRefreshSearch = () => SearchActions.refresh();

jest.mock('views/components/DashboardSearchBar', () => () => (
  <button type="button" onClick={mockRefreshSearch}>Execute Query</button>
));

jest.mock('views/stores/SearchMetadataStore');
jest.mock('views/components/views/CurrentViewTypeProvider', () => jest.fn());
jest.mock('views/hooks/SyncWithQueryParameters');

jest.mock('routing/withLocation', () => (Component) => (props) => (
  <Component location={{ query: {}, pathname: '', search: '' }} {...props} />
));

jest.mock('views/components/contexts/WidgetFieldTypesContextProvider', () => ({ children }) => children);
jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/logic/usePluginEntities');

describe('Search', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
    WidgetStore.listen = jest.fn(() => jest.fn());
    QueryFiltersStore.listen = jest.fn(() => jest.fn());
    SearchActions.execute = mockAction(jest.fn(async () => ({} as SearchExecutionResult)));
    StreamsActions.refresh = mockAction();
    SearchConfigActions.refresh = mockAction();
    SearchExecutionStateStore.listen = jest.fn(() => jest.fn());
    ViewActions.search.completed.listen = jest.fn(() => jest.fn());

    ViewStore.getInitialState = jest.fn(() => ({
      view: View.create().toBuilder().type(View.Type.Dashboard).build(),
      dirty: false,
      isNew: true,
      activeQuery: 'foobar',
    }));

    SearchMetadataActions.parseSearch = mockAction(jest.fn(() => Promise.resolve(SearchMetadata.empty())));
    SearchMetadataStore.listen = jest.fn(() => jest.fn());
    SearchActions.refresh = mockAction();

    asMock(CurrentViewTypeProvider as React.FunctionComponent).mockImplementation(({ children }) => <ViewTypeContext.Provider value={View.Type.Dashboard}>{children}</ViewTypeContext.Provider>);

    const query = Query.builder().id('foobar').filter(filtersForQuery([])).build();
    asMock(useCurrentQuery).mockReturnValue(query);
  });

  it('register a WindowLeaveMessage', async () => {
    render(<Search />);

    await waitFor(() => expect(WindowLeaveMessage).toHaveBeenCalled());
  });

  it('executes search upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(SearchActions.execute).toHaveBeenCalled());
  });

  it('refreshes search config upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(SearchConfigActions.refresh).toHaveBeenCalled());
  });

  it('does not register to QueryFiltersStore upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(QueryFiltersStore.listen).not.toHaveBeenCalled());
  });

  it('does not unregister from Query Filter store upon unmount', () => {
    const unsubscribe = jest.fn();

    QueryFiltersStore.listen = jest.fn(() => unsubscribe);
    const { unmount } = render(<Search />);

    unmount();

    expect(unsubscribe).not.toHaveBeenCalled();
  });

  it('registers to SearchActions.refresh upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(SearchActions.refresh.listen).toHaveBeenCalled());
  });

  it('registers to ViewActions.search.completed upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(ViewActions.search.completed.listen).toHaveBeenCalled());
  });

  it('unregisters from ViewActions.search.completed upon unmount', async () => {
    const unsubscribe = jest.fn();

    ViewActions.search.completed.listen = jest.fn(() => unsubscribe);
    const { unmount } = render(<Search />);

    await waitFor(() => expect(ViewActions.search.completed.listen).toHaveBeenCalled());

    expect(unsubscribe).not.toHaveBeenCalled();

    unmount();

    await waitFor(() => expect(unsubscribe).toHaveBeenCalled());
  });

  it('refreshes Streams upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(StreamsActions.refresh).toHaveBeenCalled());
  });

  it('synchronizes URL upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(useSyncWithQueryParameters).toHaveBeenCalled());
  });

  it('updating search in view triggers search execution', async () => {
    render(<Search />);

    asMock(SearchActions.execute).mockClear();

    expect(SearchActions.execute).not.toHaveBeenCalled();

    ViewActions.search({} as View['search']);

    await waitFor(() => expect(SearchActions.execute).toHaveBeenCalled());
  });

  it('refreshing after query change parses search metadata first', async () => {
    render(<Search />);

    const executeQuery = await screen.findByRole('button', { name: 'Execute Query' });

    fireEvent.click(executeQuery);

    await waitFor(() => expect(SearchMetadataActions.parseSearch).toHaveBeenCalled());

    expect(SearchActions.execute).toHaveBeenCalled();
  });
});
