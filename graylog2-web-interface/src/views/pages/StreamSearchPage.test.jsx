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
import { render, waitFor, fireEvent } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';
import asMock from 'helpers/mocking/AsMock';

import StreamsContext from 'contexts/StreamsContext';
import { processHooks } from 'views/logic/views/ViewLoader';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import Search from 'views/logic/search/Search';
import SearchComponent from 'views/components/Search';
import { loadNewViewForStream, loadView } from 'views/logic/views/Actions';

import StreamSearchPage from './StreamSearchPage';

const mockView = View.create()
  .toBuilder()
  .type(View.Type.Search)
  .search(Search.builder().build())
  .build();

jest.mock('views/components/Search', () => jest.fn(() => <div>Extended search page</div>));
jest.mock('views/stores/SearchStore', () => ({ SearchActions: {} }));

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    create: jest.fn(() => Promise.resolve({ view: mockView })),
  },
}));

jest.mock('routing/withLocation', () => (x) => x);
jest.mock('routing/withParams', () => (x) => x);

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    get: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/logic/views/ViewLoader', () => ({
  processHooks: jest.fn((promise, loadHooks, executeHooks, query, onSuccess) => Promise.resolve().then(onSuccess)),
}));

jest.mock('views/hooks/SyncWithQueryParameters');

jest.mock('views/logic/views/Actions');

describe('StreamSearchPage', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  const mockRouter = {
    getCurrentLocation: jest.fn(() => ({ pathname: '/search', search: '?q=&rangetype=relative&relative=300' })),
  };
  const mockLocation = {
    pathname: '/search',
    query: {
      q: '',
      rangetype: 'relative',
      relative: '300',
    },
  };
  const SimpleStreamSearchPage = (props) => (
    <StreamsContext.Provider value={[{ id: 'stream-id-1' }]}>
      <StreamSearchPage location={{ query: {} }}
                        params={{ streamId: 'stream-id-1' }}
                        router={mockRouter}
                        {...props} />
    </StreamsContext.Provider>
  );

  afterEach(() => {
    jest.clearAllMocks();
    jest.resetModules();
  });

  it('shows loading spinner before rendering page', async () => {
    const { findByText, getByText } = render(<SimpleStreamSearchPage />);
    act(() => jest.advanceTimersByTime(200));

    expect(getByText('Loading...')).not.toBeNull();

    await findByText('Extended search page');
  });

  it('should create view with streamId passed from props', async () => {
    render(<SimpleStreamSearchPage />);
    await waitFor(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-1'));
  });

  it('should recreate view when streamId passed from props changes', async () => {
    const { rerender } = render(<SimpleStreamSearchPage />);

    await waitFor(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-1'));

    rerender(<SimpleStreamSearchPage params={{ streamId: 'stream-id-2' }} />);

    await waitFor(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-2'));
  });

  describe('loading another view', () => {
    it('should be possible with specific view id', async () => {
      asMock(SearchComponent).mockImplementationOnce(() => (
        <ViewLoaderContext.Consumer>
          {(_loadView) => <button type="button" onClick={() => _loadView && _loadView('special-view-id')}>Load view</button>}
        </ViewLoaderContext.Consumer>
      ));

      const { findByText } = render(<SimpleStreamSearchPage />);
      const viewLoadButton = await findByText('Load view');
      fireEvent.click(viewLoadButton);

      await waitFor(() => expect(loadView).toHaveBeenCalled());

      expect(loadView).toHaveBeenCalledWith('special-view-id');
    });
  });

  describe('loading new empty view', () => {
    beforeEach(() => {
      asMock(SearchComponent).mockImplementationOnce(() => (
        <NewViewLoaderContext.Consumer>
          {(loadNewView) => <button type="button" onClick={() => loadNewView()}>Load new view</button>}
        </NewViewLoaderContext.Consumer>
      ));
    });

    it('should be supported', async () => {
      const { findByText } = render(<SimpleStreamSearchPage />);
      const viewCreateButton = await findByText('Load new view');

      fireEvent.click(viewCreateButton);

      await waitFor(() => expect(loadNewViewForStream).toHaveBeenCalled());

      expect(loadNewViewForStream).toHaveBeenCalledWith('stream-id-1');
    });

    it('should process hooks with empty query', async () => {
      const processHooksAction = asMock(processHooks);
      const { findByText } = render(<SimpleStreamSearchPage location={mockLocation} />);
      const viewCreateButton = await findByText('Load new view');

      fireEvent.click(viewCreateButton);

      await waitFor(() => expect(processHooksAction).toHaveBeenCalled());

      const query = {
        q: '',
        rangetype: 'relative',
        relative: '300',
      };

      expect(processHooksAction).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything(), query, expect.anything());
    });
  });
});
