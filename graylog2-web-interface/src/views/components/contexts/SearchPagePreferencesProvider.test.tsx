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
import { MockStore } from 'helpers/mocking';
import CurrentUserProvider from 'contexts/CurrentUserProvider';
import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';
import Store from 'logic/local-storage/Store';
import View from 'views/logic/views/View';
import CurrentViewTypeProvider from 'views/components/views/CurrentViewTypeProvider';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import type { UserJSON } from 'logic/users/User';
import { PreferencesActions } from 'stores/users/PreferencesStore';

import SearchPagePreferencesContext from './SearchPagePreferencesContext';
import SearchPagePreferencesProvider from './SearchPagePreferencesProvider';

jest.mock('stores/users/CurrentUserStore', () => ({
  CurrentUserStore: MockStore(),
}));

jest.mock('stores/users/PreferencesStore', () => ({
  PreferencesActions: {
    list: jest.fn(),
    saveUserPreferences: jest.fn(),
  },
  PreferencesStore: MockStore(),
}));

jest.mock('logic/local-storage/Store', () => ({
  get: jest.fn(),
  set: jest.fn(),
}));

describe('SearchPagePreferencesProvider', () => {
  const SimpleProvider = ({ children }: { children: any }) => (
    <CurrentUserProvider>
      <CurrentUserPreferencesProvider>
        <CurrentViewTypeProvider type={View.Type.Search}>
          <SearchPagePreferencesProvider>
            <SearchPagePreferencesContext.Consumer>
              {children}
            </SearchPagePreferencesContext.Consumer>
          </SearchPagePreferencesProvider>
        </CurrentViewTypeProvider>
      </CurrentUserPreferencesProvider>
    </CurrentUserProvider>
  );

  const ProviderWithToggleButton = () => (
    <SimpleProvider>
      {(searchPagePreferences) => {
        if (!searchPagePreferences) return '';
        const { actions: { toggleSidebarPinning } } = searchPagePreferences;

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

  it('provides default search page preference state with empty preference store', () => {
    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(false);
  });

  it('provides default search page preference state if user does not exists', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: {} as UserJSON });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(false);
  });

  it('provides default search page preference state if user has no preferences', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: { preferences: {} } as UserJSON });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(false);
  });

  it('provides search page preferences based on user preferences', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        preferences: {
          searchSidebarIsPinned: true,
        },
      } as UserJSON,
    });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(true);
  });

  it('provides search page preference state based on local storage for system admin', () => {
    asMock(Store.get).mockImplementationOnce((key) => {
      if (key === 'searchSidebarIsPinned') return true;

      return false;
    });

    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        id: 'local:admin',
        username: 'admin',
        read_only: true,
      } as UserJSON,
    });

    const consume = renderSUT();

    expect(consume.mock.calls[0][0]?.config.sidebar.isPinned).toEqual(true);
  });

  it('should update user preferences on state change', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        username: 'alice',
      } as UserJSON,
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

  it('should update local storage on preference state change for system admin', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        id: 'local:admin',
        username: 'admin',
        read_only: true,
      } as UserJSON,
    });

    const { getByText } = render(<ProviderWithToggleButton />);

    fireEvent.click(getByText('Toggle sidebar pinning'));

    expect(Store.set).toHaveBeenCalledTimes(1);

    expect(Store.set).toHaveBeenCalledWith('searchSidebarIsPinned', true);
  });
});
