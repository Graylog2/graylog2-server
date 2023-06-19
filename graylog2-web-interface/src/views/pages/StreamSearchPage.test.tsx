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

import asMock from 'helpers/mocking/AsMock';
import StreamsContext from 'contexts/StreamsContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import SearchComponent from 'views/components/Search';
import { loadNewViewForStream, loadView } from 'views/logic/views/Actions';
import useParams from 'routing/useParams';
import useQuery from 'routing/useQuery';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import { createSearch } from 'fixtures/searches';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import useCreateSearch from 'views/hooks/useCreateSearch';
import type View from 'views/logic/views/View';

import StreamSearchPage from './StreamSearchPage';

const mockView = createSearch();

jest.mock('views/components/Search', () => jest.fn(() => <div>Extended search page</div>));

jest.mock('routing/useQuery');
jest.mock('routing/useParams');

jest.mock('views/logic/views/Actions');
jest.mock('views/logic/views/UseCreateSavedSearch');
jest.mock('views/logic/views/UseProcessHooksForView');
jest.mock('views/hooks/useCreateSearch');

describe('StreamSearchPage', () => {
  const mockQuery = {
    q: '',
    rangetype: 'relative',
    relative: '300',
  };
  const streamId = 'stream-id-1';
  const SimpleStreamSearchPage = () => (
    <StreamsContext.Provider value={[{ id: streamId, title: 'Teststream' }]}>
      <StreamSearchPage />
    </StreamsContext.Provider>
  );

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useQuery).mockReturnValue({});
    asMock(useParams).mockReturnValue({ streamId });
    asMock(useCreateSavedSearch).mockReturnValue(Promise.resolve(mockView));
    asMock(useProcessHooksForView).mockReturnValue({ status: 'loaded', view: mockView, executionState: SearchExecutionState.empty() });
    asMock(useCreateSearch).mockImplementation(async (view: Promise<View>) => view);
  });

  it('shows loading spinner before rendering page', async () => {
    asMock(useProcessHooksForView).mockReturnValue({ status: 'loading' });

    const { findByText } = render(<SimpleStreamSearchPage />);

    expect(await findByText('Loading...')).toBeInTheDocument();
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
      asMock(SearchComponent as React.FunctionComponent).mockImplementation(() => (
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

      expect(loadView).toHaveBeenCalledWith(expect.anything(), 'special-view-id');
    });
  });

  describe('loading new empty view', () => {
    beforeEach(() => {
      asMock(SearchComponent as React.FunctionComponent).mockImplementation(() => (
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

      expect(loadNewViewForStream).toHaveBeenCalledWith(expect.anything(), 'stream-id-1');
    });

    it('should process hooks with empty query', async () => {
      asMock(useQuery).mockReturnValue(mockQuery);
      const { findByText } = render(<SimpleStreamSearchPage />);
      const viewCreateButton = await findByText('Load new view');

      fireEvent.click(viewCreateButton);

      await waitFor(() => expect(useProcessHooksForView).toHaveBeenCalled());

      expect(useProcessHooksForView).toHaveBeenCalledWith(expect.anything(), SearchExecutionState.empty(), {
        q: '',
        rangetype: 'relative',
        relative: '300',
      });
    });
  });
});
