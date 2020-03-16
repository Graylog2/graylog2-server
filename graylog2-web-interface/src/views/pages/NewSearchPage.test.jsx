// @flow strict
import * as React from 'react';

import { render, cleanup, wait, waitForElement, fireEvent } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';

import { processHooks } from 'views/logic/views/ViewLoader';
import { syncWithQueryParameters } from 'views/hooks/SyncWithQueryParameters';
import { ViewActions } from 'views/stores/ViewStore';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';

import NewSearchPage from './NewSearchPage';
import { act } from "react-dom/test-utils";

const mockExtendedSearchPage = jest.fn(() => <div>Extended search page</div>);
const mockView = View.create()
  .toBuilder()
  .type(View.Type.Search)
  .search(Search.builder().build())
  .build();

jest.mock('react-router', () => ({ withRouter: x => x }));
jest.mock('./ExtendedSearchPage', () => mockExtendedSearchPage);
jest.mock('views/stores/SearchStore', () => {});
jest.mock('views/stores/ViewStore', () => ({
  ViewActions: { create: jest.fn(() => Promise.resolve({ view: mockView })) },
}));
jest.mock('views/hooks/SyncWithQueryParameters', () => ({
  syncWithQueryParameters: jest.fn(),
}));
jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    get: jest.fn(() => Promise.resolve()),
  },
}));
jest.mock('views/logic/views/ViewLoader', () => {
  const originalModule = jest.requireActual('views/logic/views/ViewLoader');
  return {
    __esModule: true,
    ...originalModule,
    processHooks: jest.fn((promise, loadHooks, executeHooks, query, onSuccess) => Promise.resolve().then(onSuccess)),
  };
});

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
  const SimpleNewSearchPage = props => <NewSearchPage route={{}} router={mockRouter} location={{}} {...props} />;

  beforeAll(() => {
    jest.useFakeTimers();
  });
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('should render minimal', async () => {
    const { getByText } = render(<SimpleNewSearchPage />);
    await waitForElement(() => getByText('Extended search page'));
  });

  it('should show spinner while loading view', () => {
    const { getByText } = render(<SimpleNewSearchPage />);
    act(() => jest.advanceTimersByTime(200));
    expect(getByText('Loading...')).not.toBeNull();
  });

  describe('mounting', () => {
    it('should create new view with type search', async () => {
      render(<SimpleNewSearchPage />);

      await wait(() => expect(ViewActions.create).toBeCalledTimes(1));
      await wait(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search));
    });

    it('should process hooks with provided location query', async () => {
      const processHooksAction = asMock(processHooks);
      render(<SimpleNewSearchPage location={mockLocation} />);

      await wait(() => expect(processHooksAction).toBeCalledTimes(1));
      await wait(() => expect(processHooksAction.mock.calls[0][3]).toStrictEqual({ q: '', rangetype: 'relative', relative: '300' }));
    });
  });

  describe('loading another view', () => {
    it('should be possible with specific view id', async () => {
      mockExtendedSearchPage.mockImplementationOnce(() => (
        <ViewLoaderContext.Consumer>
          {loadView => <button type="button" onClick={() => loadView && loadView('special-view-id')}>Load view</button>}
        </ViewLoaderContext.Consumer>
      ));

      const { getByText } = render(<SimpleNewSearchPage />);
      const viewLoadButton = await waitForElement(() => getByText('Load view'));
      fireEvent.click(viewLoadButton);

      await wait(() => expect(ViewManagementActions.get).toHaveBeenCalledTimes(1));
      await wait(() => expect(ViewManagementActions.get).toHaveBeenCalledWith('special-view-id'));
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
      const { getByText } = render(<SimpleNewSearchPage />);
      const viewCreateButton = await waitForElement(() => getByText('Load new view'));
      fireEvent.click(viewCreateButton);

      await wait(() => expect(ViewActions.create).toBeCalledTimes(2));
      await wait(() => expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search));
    });

    it('should process hooks with empty query', async () => {
      const processHooksAction = asMock(processHooks);
      const { getByText } = render(<SimpleNewSearchPage location={mockLocation} />);
      const viewCreateButton = await waitForElement(() => getByText('Load new view'));
      fireEvent.click(viewCreateButton);

      await wait(() => expect(processHooksAction).toBeCalledTimes(2));
      await wait(() => expect(processHooksAction.mock.calls[1][3]).toStrictEqual({}));
    });

    it('should sync query params with current url', async () => {
      const { getByText } = render(<SimpleNewSearchPage location={mockLocation} />);
      const viewCreateButton = await waitForElement(() => getByText('Load new view'));
      fireEvent.click(viewCreateButton);

      await wait(() => expect(syncWithQueryParameters).toBeCalledTimes(1));
      await wait(() => expect(syncWithQueryParameters).toHaveBeenCalledWith('/search?q=&rangetype=relative&relative=300'));
    });
  });
});
