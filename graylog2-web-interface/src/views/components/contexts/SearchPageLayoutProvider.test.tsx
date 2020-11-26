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
import { render, fireEvent } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';
import { MockCombinedProvider, MockStore } from 'helpers/mocking';

import CurrentUserProvider from 'contexts/CurrentUserProvider';
import CombinedProvider from 'injection/CombinedProvider';
import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';
import Store from 'logic/local-storage/Store';
import View from 'views/logic/views/View';
import CurrentViewTypeProvider from 'views/components/views/CurrentViewTypeProvider';

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

describe('SearchPageLayoutProvider', () => {
  const SimpleProvider = ({ children }: { children: any }) => (
    <CurrentUserProvider>
      <CurrentUserPreferencesProvider>
        <CurrentViewTypeProvider type={View.Type.Search}>
          <SearchPageLayoutProvider>
            <SearchPageLayoutContext.Consumer>
              {children}
            </SearchPageLayoutContext.Consumer>
          </SearchPageLayoutProvider>
        </CurrentViewTypeProvider>
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

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(false);
  });

  it('provides default search page layout if user does not exists', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: {} });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(false);
  });

  it('provides default search page layout if user has no preferences', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: { preferences: {} } });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(false);
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

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(true);
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
        read_only: true,
      },
    });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(true);
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
      {
        enableSmartSearch: true,
        updateUnfocussed: false,
        searchSidebarIsPinned: true,
        dashboardSidebarIsPinned: false,
        themeMode: 'teint',
      },
      undefined,
      false,
    );
  });

  it('should update local storage on layout change for system admin', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        id: 'local:admin',
        username: 'admin',
        read_only: true,
      },
    });

    const { getByText } = render(<ProviderWithToggleButton />);

    fireEvent.click(getByText('Toggle sidebar pinning'));

    expect(Store.set).toHaveBeenCalledTimes(1);

    expect(Store.set).toHaveBeenCalledWith('searchSidebarIsPinned', true);
  });
});
