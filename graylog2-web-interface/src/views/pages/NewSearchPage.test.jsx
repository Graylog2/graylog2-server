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
import { act } from 'react-dom/test-utils';
import { render, waitFor, fireEvent } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';

import { processHooks } from 'views/logic/views/ViewLoader';
import { ViewActions } from 'views/stores/ViewStore';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import Search from 'views/logic/search/Search';
import SearchComponent from 'views/components/Search';
import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import StreamsContext from 'contexts/StreamsContext';
import { loadNewView, loadView } from 'views/logic/views/Actions';

import NewSearchPage from './NewSearchPage';

const mockView = View.create()
  .toBuilder()
  .type(View.Type.Search)
  .search(Search.builder().build())
  .build();

jest.mock('routing/withLocation', () => (x) => x);
jest.mock('views/components/Search', () => jest.fn(() => <div>Extended search page</div>));
jest.mock('views/stores/SearchStore');

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: { create: jest.fn(() => Promise.resolve({ view: mockView })) },
}));

jest.mock('views/hooks/SyncWithQueryParameters');

jest.mock('views/stores/ViewManagementStore');

jest.mock('views/logic/views/ViewLoader', () => ({
  processHooks: jest.fn((promise, loadHooks, executeHooks, query, onSuccess) => Promise.resolve().then(onSuccess)),
}));

jest.mock('views/logic/views/Actions');

describe('NewSearchPage', () => {
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
  const SimpleNewSearchPage = (props) => (
    <StreamsContext.Provider value={[{}]}>
      <NewSearchPage router={mockRouter} location={{ pathname: '/foo', query: {} }} {...props} />
    </StreamsContext.Provider>
  );

  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render minimal', async () => {
    const { findByText } = render(<SimpleNewSearchPage />);

    await findByText('Extended search page');
  });

  it('should show spinner while loading view', async () => {
    const { findByText, getByText } = render(<SimpleNewSearchPage />);

    act(() => jest.advanceTimersByTime(200));

    expect(getByText('Loading...')).not.toBeNull();

    await findByText('Extended search page');
  });

  describe('mounting', () => {
    it('should create new view with type search', async () => {
      render(<SimpleNewSearchPage />);

      await waitFor(() => expect(ViewActions.create).toBeCalledTimes(1));

      expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, undefined);
    });

    it('should process hooks with provided location query', async () => {
      const processHooksAction = asMock(processHooks);

      render(<SimpleNewSearchPage location={mockLocation} />);

      await waitFor(() => expect(processHooksAction).toBeCalledTimes(1));
      await waitFor(() => expect(processHooksAction.mock.calls[0][3]).toStrictEqual({ q: '', rangetype: 'relative', relative: '300' }));
    });
  });

  describe('loading another view', () => {
    it('should be possible with specific view id', async () => {
      asMock(SearchComponent).mockImplementationOnce(() => (
        <ViewLoaderContext.Consumer>
          {(_loadView) => <button type="button" onClick={() => _loadView && _loadView('special-view-id')}>Load view</button>}
        </ViewLoaderContext.Consumer>
      ));

      const { findByText } = render(<SimpleNewSearchPage />);
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
          {(_loadNewView) => <button type="button" onClick={() => _loadNewView()}>Load new view</button>}
        </NewViewLoaderContext.Consumer>
      ));
    });

    it('should be supported', async () => {
      const { findByText } = render(<SimpleNewSearchPage />);
      const viewCreateButton = await findByText('Load new view');
      fireEvent.click(viewCreateButton);

      await waitFor(() => expect(loadNewView).toBeCalled());
    });

    it('should process hooks with query', async () => {
      const processHooksAction = asMock(processHooks);
      const { findByText } = render(<SimpleNewSearchPage location={mockLocation} />);
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
