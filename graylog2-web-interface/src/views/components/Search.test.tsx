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
import * as Immutable from 'immutable';
import { render, waitFor } from 'wrappedTestingLibrary';
import { PluginStore } from 'graylog-web-plugin/plugin';

import mockComponent from 'helpers/mocking/MockComponent';
import mockAction from 'helpers/mocking/MockAction';
import { StreamsActions } from 'views/stores/StreamsStore';
import { WidgetStore } from 'views/stores/WidgetStore';
import { SearchActions } from 'views/stores/SearchStore';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchConfigActions } from 'views/stores/SearchConfigStore';
import { ViewActions, ViewStore } from 'views/stores/ViewStore';
import { SearchMetadataActions, SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import View from 'views/logic/views/View';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import type { SearchExecutionResult } from 'views/actions/SearchActions';
import WindowLeaveMessage from 'views/components/common/WindowLeaveMessage';
import PluggableStoreProvider from 'components/PluggableStoreProvider';
import viewsReducers from 'views/viewsReducers';
import ViewState from 'views/logic/views/ViewState';
import SearchModel from 'views/logic/search/Search';

import OriginalSearch from './Search';

import { useSyncWithQueryParameters } from '../hooks/SyncWithQueryParameters';

jest.mock('util/History');

jest.mock('views/logic/fieldtypes/useFieldTypes');

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
jest.mock('views/components/SearchBar', () => mockComponent('SearchBar'));

const mockRefreshSearch = () => SearchActions.refresh();

jest.mock('views/components/DashboardSearchBar', () => () => (
  <button type="button" onClick={mockRefreshSearch}>Execute Query</button>
));

jest.mock('views/hooks/SyncWithQueryParameters');

jest.mock('routing/withLocation', () => (Component) => (props) => (
  <Component location={{ query: {}, pathname: '', search: '' }} {...props} />
));

jest.mock('views/components/contexts/WidgetFieldTypesContextProvider', () => ({ children }) => children);

const view = View.create()
  .toBuilder()
  .type(View.Type.Dashboard)
  .state(Immutable.Map({ foobar: ViewState.create() }))
  .search(SearchModel.create())
  .build();

const Search = () => (
  <PluggableStoreProvider view={view} initialQuery="foobar" isNew>
    <OriginalSearch />
  </PluggableStoreProvider>
);

const plugin = {
  exports: {
    'views.reducers': viewsReducers,
  },
  metadata: {
    name: 'Dummy Plugin for Tests',
  },
};

describe('Search', () => {
  beforeAll(() => PluginStore.register(plugin));

  afterAll(() => PluginStore.unregister(plugin));

  beforeEach(() => {
    WidgetStore.listen = jest.fn(() => jest.fn());
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
  });

  it('register a WindowLeaveMessage', async () => {
    render(<Search />);

    await waitFor(() => expect(WindowLeaveMessage).toHaveBeenCalled());
  });

  it('refreshes search config upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(SearchConfigActions.refresh).toHaveBeenCalled());
  });

  it('refreshes Streams upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(StreamsActions.refresh).toHaveBeenCalled());
  });

  it('synchronizes URL upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(useSyncWithQueryParameters).toHaveBeenCalled());
  });
});
