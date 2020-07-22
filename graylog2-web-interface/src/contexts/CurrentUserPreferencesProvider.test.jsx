// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';
import { MockCombinedProvider, MockStore } from 'helpers/mocking';

import CombinedProvider from 'injection/CombinedProvider';

import UserPreferencesContext, { defaultUserPreferences, type UserPreferences } from './UserPreferencesContext';
import CurrentUserProvider from './CurrentUserProvider';
import CurrentUserPreferencesProvider from './CurrentUserPreferencesProvider';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

jest.mock('injection/CombinedProvider', () => new MockCombinedProvider({ CurrentUser: { CurrentUserStore: MockStore() } }));

describe('CurrentUserPreferencesProvider', () => {
  const SimpleCurrentUserPreferencesProvider = ({ children }: {children: JestMockFn<*, *>}) => (
    <CurrentUserPreferencesProvider>
      <UserPreferencesContext.Consumer>
        {children}
      </UserPreferencesContext.Consumer>
    </CurrentUserPreferencesProvider>
  );

  const renderSUT = (): ((UserPreferences: UserPreferences) => null) => {
    const consume = jest.fn();

    render(
      <CurrentUserProvider>
        <SimpleCurrentUserPreferencesProvider>
          {consume}
        </SimpleCurrentUserPreferencesProvider>
      </CurrentUserProvider>,
    );

    return consume;
  };

  it('provides default user preferences when CurrentUserContext is not provided', () => {
    const consume = jest.fn();

    render(<SimpleCurrentUserPreferencesProvider>{consume}</SimpleCurrentUserPreferencesProvider>);

    expect(consume).toHaveBeenCalledWith(defaultUserPreferences);
  });

  it('provides default user preferences with empty store', () => {
    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(defaultUserPreferences);
  });

  it('provides default user preferences if the user has none', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: {} });

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(defaultUserPreferences);
  });

  it('provides empty user preferences of current user', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: { preferences: {} } });

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith({});
  });

  it('provides user preferences of current user', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({
      currentUser: {
        preferences: {
          enableSmartSearch: false,
          updateUnfocussed: true,
        },
      },
    });

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith({
      enableSmartSearch: false,
      updateUnfocussed: true,
    });
  });
});
