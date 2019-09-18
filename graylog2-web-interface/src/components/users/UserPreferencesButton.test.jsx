import React from 'react';
import { render, fireEvent } from '@testing-library/react';
// import { shallow } from 'enzyme';

import { CombinedProviderMock, StoreMock } from 'helpers/mocking';

import UserPreferencesButton from 'components/users/UserPreferencesButton';

describe('UserPreferencesButton', () => {
  beforeEach(() => {
    jest.resetModules();
  });

  it('should load user data when user clicks edit button', () => {
    const PreferencesStore = StoreMock('get', 'listen', 'loadUserPreferences');
    const combinedProviderMock = new CombinedProviderMock({
      Preferences: { PreferencesStore },
    });

    jest.doMock('injection/CombinedProvider', () => combinedProviderMock);

    const instance = render(<UserPreferencesButton userName="Full" />);
    const button = instance.getByTestId('user-preferences-button');

    expect(instance).toMatchSnapshot();
    expect(button).toBeDefined();

    fireEvent.click(button);

    expect(PreferencesStore.loadUserPreferences).toBeCalled();
  });
});
