// @flow strict
import * as React from 'react';
import { cleanup, render } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';
import { MockCombinedProvider, MockStore } from 'helpers/mocking';

import CurrentUserProvider from 'contexts/CurrentUserProvider';
import CombinedProvider from 'injection/CombinedProvider';
import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';

import SearchPageLayoutContext from './SearchPageLayoutContext';
import SearchPageLayoutProvider, { defaultLayoutConfig } from './SearchPageLayoutProvider';

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

describe('CurrentUserPreferencesProvider', () => {
  afterEach(cleanup);

  const renderSUT = () => {
    const consume = jest.fn();

    render(
      <CurrentUserProvider>
        <CurrentUserPreferencesProvider>
          <SearchPageLayoutProvider>
            <SearchPageLayoutContext.Consumer>
              {consume}
            </SearchPageLayoutContext.Consumer>
          </SearchPageLayoutProvider>
        </CurrentUserPreferencesProvider>
      </CurrentUserProvider>,
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
});
