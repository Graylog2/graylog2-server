// @flow strict
import * as React from 'react';
import { cleanup, render, fireEvent } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';
import { MockCombinedProvider, MockStore } from 'helpers/mocking';

import CurrentUserProvider from 'contexts/CurrentUserProvider';
import CombinedProvider from 'injection/CombinedProvider';
import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';
import Store from 'logic/local-storage/Store';

import SearchPageLayoutContext from './SearchPageLayoutContext';
import SearchPageLayoutProvider from './SearchPageLayoutProvider';

const { PreferencesActions } = CombinedProvider.get('Preferences');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

jest.mock('injection/CombinedProvider', () => new MockCombinedProvider({
  CurrentUser: {
    CurrentUserStore: MockStore(),
  },
  Preferences: {
    PreferencesActions: {
      list: jest.fn(),
      saveUserPreferences: jest.fn(),
    },
    PreferencesStore: MockStore(),
  },
}));

jest.mock('logic/local-storage/Store', () => ({
  get: jest.fn(),
  set: jest.fn(),
}));

describe('CurrentUserPreferencesProvider', () => {
  afterEach(cleanup);

  const SimpleProvider = ({ children }: { children: any }) => (
    <CurrentUserProvider>
      <CurrentUserPreferencesProvider>
        <SearchPageLayoutProvider>
          <SearchPageLayoutContext.Consumer>
            {children}
          </SearchPageLayoutContext.Consumer>
        </SearchPageLayoutProvider>
      </CurrentUserPreferencesProvider>
    </CurrentUserProvider>
  );

  const ProviderWithToggleButton = () => (
    <SimpleProvider>
      {(searchPageLayout) => {
        if (!searchPageLayout) return '';
        const { actions: { toggleSidebarPinning } } = searchPageLayout;

        return (<button type="button" onClick={() => toggleSidebarPinning()}>Toggle sidebar pinning</button>);
      }}
    </SimpleProvider>
  );

  const renderSUT = () => {
    const consume = jest.fn();

    render(
      <SimpleProvider>
        {consume}
      </SimpleProvider>,
    );

    return consume;
  };

  it('provides default search page layout with empty preference store', () => {
    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.dashboardSidebarIsPinned).toEqual(false);
    expect(consume.mock.calls[0][0]?.config.sidebar.searchSidebarIsPinned).toEqual(false);
  });

  it('provides default search page layout if user does not exists', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: {} });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.dashboardSidebarIsPinned).toEqual(false);
    expect(consume.mock.calls[0][0]?.config.sidebar.searchSidebarIsPinned).toEqual(false);
  });

  it('provides default search page layout if user has no preferences', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: { preferences: {} } });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.dashboardSidebarIsPinned).toEqual(false);
    expect(consume.mock.calls[0][0]?.config.sidebar.searchSidebarIsPinned).toEqual(false);
  });

  it('provides search page layout based on user preferences', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        preferences: {
          searchSidebarIsPinned: true,
        },
      },
    });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.searchSidebarIsPinned).toEqual(true);
  });

  it('provides search page layout based on local storage for system admin', () => {
    asMock(Store.get).mockImplementationOnce((key) => {
      if (key === 'searchSidebarIsPinned') return true;

      return false;
    });

    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        id: 'local:admin',
        username: 'admin',
      },
    });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.searchSidebarIsPinned).toEqual(true);
  });

  it('should update user preferences on layout change', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        username: 'alice',
      },
    });

    const { getByText } = render(<ProviderWithToggleButton />);

    fireEvent.click(getByText('Toggle sidebar pinning'));

    expect(PreferencesActions.saveUserPreferences).toHaveBeenCalledTimes(1);

    expect(PreferencesActions.saveUserPreferences).toHaveBeenCalledWith(
      'alice',
      [
        { name: 'enableSmartSearch', value: true },
        { name: 'updateUnfocussed', value: false },
        { name: 'searchSidebarIsPinned', value: true },
        { name: 'dashboardSidebarIsPinned', value: false },
      ],
      undefined,
      false,
    );
  });

  it('should update local storage on layout change for system admin', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        id: 'local:admin',
        username: 'admin',
      },
    });

    const { getByText } = render(<ProviderWithToggleButton />);

    fireEvent.click(getByText('Toggle sidebar pinning'));

    expect(Store.set).toHaveBeenCalledTimes(1);

    expect(Store.set).toHaveBeenCalledWith('searchSidebarIsPinned', true);
  });
});
