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
import { render, waitFor, fireEvent } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import asMock from 'helpers/mocking/AsMock';
import { MockStore } from 'helpers/mocking';
import StreamsContext from 'contexts/StreamsContext';
import processHooks from 'views/logic/views/processHooks';
import View from 'views/logic/views/View';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import Search from 'views/logic/search/Search';
import SearchComponent from 'views/components/Search';
import { loadNewViewForStream, loadView } from 'views/logic/views/Actions';
import useParams from 'routing/useParams';
import useQuery from 'routing/useQuery';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';

import StreamSearchPage from './StreamSearchPage';

const mockView = View.create()
  .toBuilder()
  .type(View.Type.Search)
  .search(Search.builder().build())
  .build();

jest.mock('views/components/Search', () => jest.fn(() => <div>Extended search page</div>));
jest.mock('views/stores/SearchStore', () => ({ SearchActions: {} }));

jest.mock('views/stores/ViewStatesStore', () => ({
  ViewStatesStore: {
    listen: jest.fn(),
    getInitialState: jest.fn(() => ({ has: jest.fn(() => false) })),
  },
}));

jest.mock('views/stores/ViewStore', () => ({
  ViewStore: MockStore(['getInitialState', () => ({ view: mockView })]),
}));

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    get: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/hooks/SyncWithQueryParameters');

jest.mock('views/logic/views/Actions');

jest.mock('routing/useQuery');
jest.mock('routing/useParams');
jest.mock('views/logic/views/UseCreateSavedSearch');
jest.mock('views/hooks/useLoadView');
jest.mock('views/logic/views/processHooks');

describe('StreamSearchPage', () => {
  const mockQuery = {
    q: '',
    rangetype: 'relative',
    relative: '300',
  };
  const streamId = 'stream-id-1';
  const SimpleStreamSearchPage = () => (
    <StreamsContext.Provider value={[{ id: streamId }]}>
      <StreamSearchPage />
    </StreamsContext.Provider>
  );

  beforeEach(() => {
    asMock(useQuery).mockReturnValue({});
    asMock(useParams).mockReturnValue({ streamId });
    asMock(useCreateSavedSearch).mockReturnValue(Promise.resolve(mockView));
    asMock(processHooks).mockImplementation((viewPromise, _loadingViewHooks, _executingViewHook, _query, onSuccess) => viewPromise.then(onSuccess));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('shows loading spinner before rendering page', async () => {
    jest.useFakeTimers();

    const { findByText } = render(<SimpleStreamSearchPage />);
    act(() => { jest.advanceTimersByTime(200); });

    expect(await findByText('Loading...')).toBeInTheDocument();

    await findByText('Extended search page');
  });

  it('should create view with streamId passed from props', async () => {
    render(<SimpleStreamSearchPage />);
    await waitFor(() => expect(useCreateSavedSearch).toHaveBeenCalledWith(streamId, undefined, undefined));
  });

  it('should recreate view when streamId passed from props changes', async () => {
    const { rerender } = render(<SimpleStreamSearchPage />);

    await waitFor(() => expect(useCreateSavedSearch).toHaveBeenCalledWith(streamId, undefined, undefined));

    asMock(useParams).mockReturnValue({ streamId: 'stream-id-2' });

    rerender(<SimpleStreamSearchPage />);

    await waitFor(() => expect(useCreateSavedSearch).toHaveBeenLastCalledWith('stream-id-2', undefined, undefined));
  });

  describe('loading another view', () => {
    it('should be possible with specific view id', async () => {
      asMock(SearchComponent as React.FunctionComponent).mockImplementationOnce(() => (
        <ViewLoaderContext.Consumer>
          {(_loadView) => (
            <button type="button" onClick={() => _loadView && _loadView('special-view-id')}>Load
              view
            </button>
          )}
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
      asMock(SearchComponent as React.FunctionComponent).mockImplementationOnce(() => (
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
      asMock(useQuery).mockReturnValue(mockQuery);
      const { findByText } = render(<SimpleStreamSearchPage />);
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
