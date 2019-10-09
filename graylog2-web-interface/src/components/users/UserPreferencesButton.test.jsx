import React from 'react';
import { render, fireEvent } from '@testing-library/react';

import { CombinedProviderMock as MockCombinedProvider, StoreMock as MockStore } from 'helpers/mocking';

import UserPreferencesButton from 'components/users/UserPreferencesButton';

import StoreProvider from 'injection/StoreProvider';

jest.mock('injection/CombinedProvider', () => {
  const mockPreferencesStore = MockStore('get', 'listen', 'loadUserPreferences');
  const combinedProviderMock = new MockCombinedProvider({
    Preferences: { PreferencesStore: mockPreferencesStore },
  });

  return combinedProviderMock;
});

const PreferencesStore = StoreProvider.getStore('Preferences');

describe('UserPreferencesButton', () => {
  beforeEach(() => {
    jest.resetModules();
  });

  it('should load user data when user clicks edit button', () => {
    const instance = render(<UserPreferencesButton userName="Full" />);
    const button = instance.getByTestId('user-preferences-button');

    expect(instance).toMatchSnapshot();
    expect(button).toBeDefined();

    fireEvent.click(button);

    expect(PreferencesStore.loadUserPreferences).toBeCalled();
  });
});
