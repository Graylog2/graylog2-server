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
import { render, waitFor } from 'wrappedTestingLibrary';

import mockComponent from 'helpers/mocking/MockComponent';
import mockAction from 'helpers/mocking/MockAction';
import { StreamsActions } from 'views/stores/StreamsStore';
import WindowLeaveMessage from 'views/components/common/WindowLeaveMessage';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { createSearch } from 'fixtures/searches';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import asMock from 'helpers/mocking/AsMock';
import mockSearchesClusterConfig from 'fixtures/searchClusterConfig';

import OriginalSearch from './Search';

import { useSyncWithQueryParameters } from '../hooks/SyncWithQueryParameters';

jest.mock('views/logic/fieldtypes/useFieldTypes');

jest.mock('views/components/QueryBar', () => mockComponent('QueryBar'));
jest.mock('views/components/SearchResult', () => mockComponent('SearchResult'));
jest.mock('views/stores/StreamsStore');
jest.mock('views/components/common/WindowLeaveMessage', () => jest.fn(mockComponent('WindowLeaveMessage')));
jest.mock('views/components/SearchBar', () => mockComponent('SearchBar'));
jest.mock('hooks/useHotkey', () => jest.fn());

const mockRefreshSearch = jest.fn();

jest.mock('views/components/DashboardSearchBar', () => () => (
  <button type="button" onClick={mockRefreshSearch}>Execute Query</button>
));

jest.mock('views/hooks/SyncWithQueryParameters');

jest.mock('routing/withLocation', () => (Component) => (props) => (
  <Component location={{ query: {}, pathname: '', search: '' }} {...props} />
));

jest.mock('views/components/contexts/WidgetFieldTypesContextProvider', () => ({ children }) => children);

jest.mock('hooks/useSearchConfiguration');

const view = createSearch({ queryId: 'foobar' });

const Search = () => (
  <TestStoreProvider view={view} initialQuery="foobar" isNew>
    <OriginalSearch />
  </TestStoreProvider>
);

describe('Search', () => {
  useViewsPlugin();

  beforeEach(() => {
    StreamsActions.refresh = mockAction();
    asMock(useSearchConfiguration).mockReturnValue({ config: mockSearchesClusterConfig, refresh: () => {} });
  });

  it('register a WindowLeaveMessage', async () => {
    render(<Search />);

    await waitFor(() => expect(WindowLeaveMessage).toHaveBeenCalled());
  });

  it('refreshes search config upon mount', async () => {
    render(<Search />);

    await waitFor(() => expect(useSearchConfiguration).toHaveBeenCalled());
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
