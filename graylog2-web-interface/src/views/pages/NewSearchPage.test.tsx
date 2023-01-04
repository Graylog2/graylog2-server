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
import { render, waitFor, fireEvent, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import mockComponent from 'helpers/mocking/MockComponent';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import Search from 'views/logic/search/Search';
import SearchComponent from 'views/components/Search';
import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import StreamsContext from 'contexts/StreamsContext';
import { loadNewView, loadView } from 'views/logic/views/Actions';
import useQuery from 'routing/useQuery';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';

import NewSearchPage from './NewSearchPage';

const mockView = View.create()
  .toBuilder()
  .type(View.Type.Search)
  .search(Search.builder().build())
  .build();

jest.mock('views/components/Search', () => jest.fn(() => <div>Extended search page</div>));
jest.mock('components/common/PublicNotifications', () => mockComponent('PublicNotifications'));
jest.mock('views/stores/SearchStore');
jest.mock('routing/useQuery');

jest.mock('views/hooks/SyncWithQueryParameters');

jest.mock('views/logic/views/Actions');
jest.mock('views/logic/views/UseCreateSavedSearch');
jest.mock('views/logic/views/UseProcessHooksForView');
jest.mock('views/hooks/useLoadView');

describe('NewSearchPage', () => {
  const query = {
    q: '',
    rangetype: 'relative',
    relative: '300',
  };
  const SimpleNewSearchPage = () => (
    <StreamsContext.Provider value={[{}]}>
      <NewSearchPage />
    </StreamsContext.Provider>
  );

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useQuery).mockReturnValue(query);
    asMock(useCreateSavedSearch).mockReturnValue(Promise.resolve(mockView));
    asMock(useProcessHooksForView).mockReturnValue([true, undefined]);
  });

  it('should render minimal', async () => {
    const { findByText } = render(<SimpleNewSearchPage />);

    await findByText('Extended search page');
  });

  it('should show spinner while loading view', async () => {
    asMock(useProcessHooksForView).mockReturnValue([false, undefined]);

    const { findByText } = render(<SimpleNewSearchPage />);

    expect(await findByText('Loading...')).not.toBeNull();
  });

  describe('mounting', () => {
    it('should create new view with type search', async () => {
      asMock(useQuery).mockReturnValue({});
      render(<SimpleNewSearchPage />);

      await waitFor(() => expect(useCreateSavedSearch).toHaveBeenCalledWith(undefined, undefined, undefined));
    });

    it('should process hooks with provided location query', async () => {
      render(<SimpleNewSearchPage />);

      await waitFor(() => expect(useProcessHooksForView).toHaveBeenCalledWith(expect.anything(), {
        q: '',
        rangetype: 'relative',
        relative: '300',
      }));
    });

    it('should display errors which occur when processing hooks', async () => {
      asMock(useProcessHooksForView).mockImplementation(() => [true, <span>An unknown error has occurred.</span>]);

      render(<SimpleNewSearchPage />);

      await screen.findByText(/An unknown error has occurred./);
    });
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

      const { findByText } = render(<SimpleNewSearchPage />);
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
          {(_loadNewView) => <button type="button" onClick={() => _loadNewView()}>Load new view</button>}
        </NewViewLoaderContext.Consumer>
      ));
    });

    it('should be supported', async () => {
      const { findByText } = render(<SimpleNewSearchPage />);
      const viewCreateButton = await findByText('Load new view');
      fireEvent.click(viewCreateButton);

      await waitFor(() => expect(loadNewView).toHaveBeenCalled());
    });

    it('should process hooks with query', async () => {
      const { findByText } = render(<SimpleNewSearchPage />);
      const viewCreateButton = await findByText('Load new view');
      fireEvent.click(viewCreateButton);

      await waitFor(() => expect(useProcessHooksForView).toHaveBeenCalled());

      expect(useProcessHooksForView).toHaveBeenCalledWith(expect.anything(), query);
    });
  });
});
