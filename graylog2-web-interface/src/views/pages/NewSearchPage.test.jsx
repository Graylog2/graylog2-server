// @flow strict
import * as React from 'react';
import { render, cleanup, wait, waitForElement, fireEvent } from 'wrappedTestingLibrary';

import { StoreMock as MockStore } from 'helpers/mocking';
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

const mockExtendedSearchPage = jest.fn(() => <div>Extended search page</div>);
const mockView = View.create()
  .toBuilder()
  .type(View.Type.Search)
  .search(Search.builder().build())
  .build();

jest.mock('views/stores/ViewStore', () => ({ ViewActions: { create: jest.fn(() => Promise.resolve({ view: mockView })) } }));
jest.mock('react-router', () => ({ withRouter: x => x }));
jest.mock('views/stores/SearchExecutionStateStore', () => MockStore());
jest.mock('views/stores/SearchStore', () => MockStore());
jest.mock('./ExtendedSearchPage', () => mockExtendedSearchPage);
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
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('should render minimal', async () => {
    const { getByText } = render(<NewSearchPage route={{}} router={mockRouter} location={{}} />);
    await waitForElement(() => getByText('Extended search page'));
  });

  it('should show spinner while loading view', () => {
    const { getByText } = render(<NewSearchPage route={{}} router={mockRouter} location={{}} />);
    expect(getByText('Loading...')).not.toBeNull();
  });

  describe('mounting', () => {
    it('should create new view with type search', async () => {
      const createViewAction = asMock(ViewActions.create);

      render(<NewSearchPage route={{}}
                            router={mockRouter}
                            location={{}} />);

      await wait(() => expect(createViewAction).toBeCalledTimes(1));
      await wait(() => expect(createViewAction).toHaveBeenCalledWith(View.Type.Search));
    });

    it('should process hooks with provided url query params', async () => {
      const processHooksAction = asMock(processHooks);

      render(<NewSearchPage route={{}}
                            router={mockRouter}
                            location={mockLocation} />);

      await wait(() => expect(processHooksAction).toBeCalledTimes(1));
      await wait(() => expect(processHooksAction.mock.calls[0][3]).toStrictEqual({ q: '', rangetype: 'relative', relative: '300' }));
    });
  });

  describe('loading another view', () => {
    it('should be possible with specific view id', async () => {
      mockExtendedSearchPage.mockImplementation(() => (
        <ViewLoaderContext.Consumer>
          {loadView => <button type="button" onClick={() => loadView && loadView('special-view-id')}>Load view</button>}
        </ViewLoaderContext.Consumer>
      ));
      const viewGetAction = asMock(ViewManagementActions.get);

      const { getByText } = render(<NewSearchPage route={{}}
                                                  router={mockRouter}
                                                  location={{}} />);
      const viewLoadButton = await waitForElement(() => getByText('Load view'));
      fireEvent.click(viewLoadButton);

      await wait(() => expect(viewGetAction).toHaveBeenCalledTimes(1));
      await wait(() => expect(viewGetAction).toHaveBeenCalledWith('special-view-id'));
    });

    // it('should show error message when loading fails', async () => {
    //   const hookComponent = <div>The hook component</div>;
    //   ViewManagementActions.get = mockAction(jest.fn(() => Promise.reject(hookComponent)));
    //   mockExtendedSearchPage.mockImplementation(() => (
    //     <ViewLoaderContext.Consumer>
    //       {loadView => loadView && <button type="button" onClick={() => loadView('special-view-id')}>Load view</button>}
    //     </ViewLoaderContext.Consumer>
    //   ));

    //   const { getByText } = render(<NewSearchPage route={{}}
    //                                               router={mockRouter}
    //                                               location={{}} />);
    //   const viewLoadButton = await waitForElement(() => getByText('Load view'));
    //   fireEvent.click(viewLoadButton);

    //   await waitForElement(() => getByText('The hook component'));
    // });
  });

  describe('loading new empty view', () => {
    it('should be supported', async () => {
      const createViewAction = asMock(ViewActions.create);
      mockExtendedSearchPage.mockImplementation(() => (
        <NewViewLoaderContext.Consumer>
          {loadNewView => <button type="button" onClick={() => loadNewView()}>Load new view</button>}
        </NewViewLoaderContext.Consumer>
      ));

      const { getByText } = render(<NewSearchPage route={{}}
                                                  router={mockRouter}
                                                  location={{}} />);
      const viewCreateButton = await waitForElement(() => getByText('Load new view'));
      fireEvent.click(viewCreateButton);

      await wait(() => expect(createViewAction).toBeCalledTimes(2));
      await wait(() => expect(createViewAction).toHaveBeenCalledWith(View.Type.Search));
    });

    it('should process hooks with empty query', async () => {
      const processHooksAction = asMock(processHooks);
      mockExtendedSearchPage.mockImplementation(() => (
        <NewViewLoaderContext.Consumer>
          {loadNewView => <button type="button" onClick={() => loadNewView()}>Load new view</button>}
        </NewViewLoaderContext.Consumer>
      ));

      const { getByText } = render(<NewSearchPage route={{}}
                                                  router={mockRouter}
                                                  location={mockLocation} />);
      const viewCreateButton = await waitForElement(() => getByText('Load new view'));
      fireEvent.click(viewCreateButton);


      await wait(() => expect(processHooksAction).toBeCalledTimes(2));
      await wait(() => expect(processHooksAction.mock.calls[1][3]).toStrictEqual({}));
    });

    it('should sync query params with current url', async () => {
      const syncWithQueryParamsAction = asMock(syncWithQueryParameters);
      mockExtendedSearchPage.mockImplementation(() => (
        <NewViewLoaderContext.Consumer>
          {loadNewView => <button type="button" onClick={() => loadNewView()}>Load new view</button>}
        </NewViewLoaderContext.Consumer>
      ));

      const { getByText } = render(<NewSearchPage route={{}}
                                                  router={mockRouter}
                                                  location={mockLocation} />);
      const viewCreateButton = await waitForElement(() => getByText('Load new view'));
      fireEvent.click(viewCreateButton);


      await wait(() => expect(syncWithQueryParamsAction).toBeCalledTimes(1));
      await wait(() => expect(syncWithQueryParamsAction).toHaveBeenCalledWith('/search?q=&rangetype=relative&relative=300'));
    });
  });
});
