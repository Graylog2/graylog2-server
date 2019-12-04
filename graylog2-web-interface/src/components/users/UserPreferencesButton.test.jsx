import React from 'react';
import { render, fireEvent } from 'wrappedTestingLibrary';

import { StoreMock as MockStore } from 'helpers/mocking';
import UserPreferencesButton from 'components/users/UserPreferencesButton';
import PreferencesStore from 'stores/users/PreferencesStore';

jest.mock('stores/users/PreferencesStore', () => MockStore('get', 'listen', 'loadUserPreferences'));

describe('UserPreferencesButton', () => {
  it('should load user data when user clicks edit button', () => {
    const instance = render(<UserPreferencesButton userName="Full" />);
    const button = instance.getByTestId('user-preferences-button');

    expect(instance.container).toMatchSnapshot();
    expect(button).toBeDefined();

    fireEvent.click(button);

    expect(PreferencesStore.loadUserPreferences).toBeCalled();
  });
});
