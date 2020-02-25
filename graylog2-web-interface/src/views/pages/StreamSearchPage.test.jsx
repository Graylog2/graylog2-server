// @flow strict
import * as React from 'react';
import { render, cleanup, wait, waitForElement, fireEvent } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';

import { processHooks } from 'views/logic/views/ViewLoader';
import { syncWithQueryParameters } from 'views/hooks/SyncWithQueryParameters';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import Search from 'views/logic/search/Search';
import StreamSearchPage from './StreamSearchPage';

const mockExtendedSearchPage = jest.fn(() => <div>Extended search page</div>);
const mockView = View.create()
  .toBuilder()
  .type(View.Type.Search)
  .search(Search.builder().build())
  .build();
jest.mock('./ExtendedSearchPage', () => mockExtendedSearchPage);
jest.mock('views/stores/SearchStore', () => ({ SearchActions: {} }));
jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    create: jest.fn(() => Promise.resolve({ view: mockView })),
  },
}));
jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    get: jest.fn(() => Promise.resolve()),
  },
}));
jest.mock('views/hooks/SyncWithQueryParameters', () => ({
  syncWithQueryParameters: jest.fn(),
}));
jest.mock('views/logic/views/ViewLoader', () => {
  const originalModule = jest.requireActual('views/logic/views/ViewLoader');
  return {
    __esModule: true,
    ...originalModule,
    processHooks: jest.fn((promise, loadHooks, executeHooks, query, onSuccess) => Promise.resolve().then(onSuccess)),
  };
});

describe('StreamSearchPage', () => {
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
  const SimpleStreamSearchPage = props => (
    <StreamSearchPage location={{ query: {} }}
                      params={{ streamId: 'stream-id-1' }}
                      route={{}}
                      router={mockRouter}
                      {...props} />
  );

  beforeEach(() => {
    cleanup();
    jest.clearAllMocks();
    jest.resetModules();
  });

  it('should render minimal', async () => {
    const { getByText } = render(<SimpleStreamSearchPage />);
    await waitForElement(() => getByText('Extended search page'));
  });

  it('should show spinner while loading view', () => {
    const { getByText } = render(<SimpleStreamSearchPage />);
    expect(getByText('Loading...')).not.toBeNull();
  });

  it('should create view with streamId passed from props', async () => {
    render(<SimpleStreamSearchPage />);
    await wait(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-1'));
  });

  it('should recreate view when streamId passed from props changes', async () => {
    const { rerender } = render(<SimpleStreamSearchPage />);

    await wait(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-1'));

    rerender(<SimpleStreamSearchPage params={{ streamId: 'stream-id-2' }} />);

    await wait(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-2'));
  });

  describe('loading another view', () => {
    it('should be possible with specific view id', async () => {
      mockExtendedSearchPage.mockImplementationOnce(() => (
        <ViewLoaderContext.Consumer>
          {loadView => <button type="button" onClick={() => loadView && loadView('special-view-id')}>Load view</button>}
        </ViewLoaderContext.Consumer>
      ));
      const viewGetAction = asMock(ViewManagementActions.get);

      const { getByText } = render(<SimpleStreamSearchPage />);
      const viewLoadButton = await waitForElement(() => getByText('Load view'));
      fireEvent.click(viewLoadButton);

      await wait(() => expect(viewGetAction).toHaveBeenCalledTimes(1));
      await wait(() => expect(viewGetAction).toHaveBeenCalledWith('special-view-id'));
    });
  });

  describe('loading new empty view', () => {
    beforeEach(() => {
      mockExtendedSearchPage.mockImplementationOnce(() => (
        <NewViewLoaderContext.Consumer>
          {loadNewView => <button type="button" onClick={() => loadNewView()}>Load new view</button>}
        </NewViewLoaderContext.Consumer>
      ));
    });

    it('should be supported', async () => {
      const { getByText } = render(<SimpleStreamSearchPage />);
      const viewCreateButton = await waitForElement(() => getByText('Load new view'));

      fireEvent.click(viewCreateButton);

      await wait(() => expect(ViewActions.create).toBeCalledTimes(2));
      await wait(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-1'));
    });

    it('should process hooks with empty query', async () => {
      const processHooksAction = asMock(processHooks);
      const { getByText } = render(<SimpleStreamSearchPage location={mockLocation} />);
      const viewCreateButton = await waitForElement(() => getByText('Load new view'));

      fireEvent.click(viewCreateButton);

      await wait(() => expect(processHooksAction).toBeCalledTimes(2));
      await wait(() => expect(processHooksAction.mock.calls[1][3]).toStrictEqual({}));
    });

    it('should sync query params with current url', async () => {
      const { getByText } = render(<SimpleStreamSearchPage location={mockLocation} />);
      const viewCreateButton = await waitForElement(() => getByText('Load new view'));

      fireEvent.click(viewCreateButton);

      await wait(() => expect(syncWithQueryParameters).toBeCalledTimes(1));
      await wait(() => expect(syncWithQueryParameters).toHaveBeenCalledWith('/search?q=&rangetype=relative&relative=300'));
    });
  });
});
