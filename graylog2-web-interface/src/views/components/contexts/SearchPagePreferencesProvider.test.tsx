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
import { defaultUser } from 'defaultMockValues';

import { adminUser, alice } from 'fixtures/users';
import asMock from 'helpers/mocking/AsMock';
import MockAction from 'helpers/mocking/MockAction';
import { MockStore } from 'helpers/mocking';
import CurrentUserPreferencesProvider from 'contexts/CurrentUserPreferencesProvider';
import Store from 'logic/local-storage/Store';
import type { PreferencesMap } from 'stores/users/PreferencesStore';
import { PreferencesActions } from 'stores/users/PreferencesStore';
import type User from 'logic/users/User';
import useCurrentUser from 'hooks/useCurrentUser';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import { createSearch } from 'fixtures/searches';
import View from 'views/logic/views/View';

import type { SearchPreferencesLayout } from './SearchPagePreferencesContext';
import SearchPagePreferencesContext from './SearchPagePreferencesContext';
import SearchPagePreferencesProvider from './SearchPagePreferencesProvider';

jest.mock('hooks/useCurrentUser');

jest.mock('stores/users/PreferencesStore', () => ({
  PreferencesActions: {
    list: jest.fn(),
    saveUserPreferences: MockAction(jest.fn()),
  },
  PreferencesStore: MockStore(),
}));

jest.mock('logic/local-storage/Store', () => ({
  get: jest.fn(),
  set: jest.fn(),
}));

describe('SearchPagePreferencesProvider', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  const view = createSearch().toBuilder().type(View.Type.Search).build();

  const SimpleProvider = ({ children }: { children: (value: SearchPreferencesLayout) => React.ReactNode}) => (
    <TestStoreProvider view={view}>
      <CurrentUserPreferencesProvider>
        <SearchPagePreferencesProvider>
          <SearchPagePreferencesContext.Consumer>
            {children}
          </SearchPagePreferencesContext.Consumer>
        </SearchPagePreferencesProvider>
      </CurrentUserPreferencesProvider>
    </TestStoreProvider>
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

    expect(consume).toHaveBeenCalledWith(expect.objectContaining({ config: expect.objectContaining({ sidebar: expect.objectContaining({ isPinned: false }) }) }));
  });

  it('provides default search page preference state if user does not exists', () => {
    asMock(useCurrentUser).mockReturnValue({} as User);
    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(expect.objectContaining({ config: expect.objectContaining({ sidebar: expect.objectContaining({ isPinned: false }) }) }));
  });

  it('provides default search page preference state if user has no preferences', () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().preferences({} as PreferencesMap).build());

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(expect.objectContaining({ config: expect.objectContaining({ sidebar: expect.objectContaining({ isPinned: false }) }) }));
  });

  it('provides search page preferences based on user preferences', () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().preferences({
      searchSidebarIsPinned: true,
    } as PreferencesMap).build());

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(expect.objectContaining({ config: expect.objectContaining({ sidebar: expect.objectContaining({ isPinned: true }) }) }));
  });

  it('provides search page preference state based on local storage for system admin', () => {
    asMock(Store.get).mockImplementationOnce((key) => {
      if (key === 'searchSidebarIsPinned') return true;

      return false;
    });

    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().readOnly(true).build());

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(expect.objectContaining({ config: expect.objectContaining({ sidebar: expect.objectContaining({ isPinned: true }) }) }));
  });

  it('should update user preferences on state change', () => {
    asMock(useCurrentUser).mockReturnValue(alice);
    const { getByText } = render(<ProviderWithToggleButton />);

    fireEvent.click(getByText('Toggle sidebar pinning'));

    expect(PreferencesActions.saveUserPreferences).toHaveBeenCalledTimes(1);

    expect(PreferencesActions.saveUserPreferences).toHaveBeenCalledWith(
      'alice',
      {
        ...alice.preferences,
        searchSidebarIsPinned: true,
      },
      undefined,
      false,
    );
  });

  it('should update local storage on preference state change for system admin', () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().readOnly(true).build());

    const { getByText } = render(<ProviderWithToggleButton />);

    fireEvent.click(getByText('Toggle sidebar pinning'));

    expect(Store.set).toHaveBeenCalledTimes(1);

    expect(Store.set).toHaveBeenCalledWith('searchSidebarIsPinned', true);
  });
});
