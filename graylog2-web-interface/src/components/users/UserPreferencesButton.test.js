import React from 'react';

import { CombinedProviderMock, StoreMock } from 'helpers/mocking';

describe('UserPreferencesButton', function () {
  beforeEach(() => {
    jest.resetModules();
  });

  it('should load user data when user clicks edit button', function () {
    const PreferencesStore = StoreMock('get', 'listen', 'loadUserPreferences');
    const combinedProviderMock = new CombinedProviderMock({
      Preferences: { PreferencesStore },
    });

    jest.doMock('injection/CombinedProvider', () => combinedProviderMock);

    const UserPreferencesButton = require('components/users/UserPreferencesButton');
    const userName = 'Full';
    const instance = require('enzyme').mount(<UserPreferencesButton userName={userName} />);

    expect(instance).toMatchSnapshot();
    expect(instance.find('button')).toBeDefined();

    instance.find('button').simulate('click');

    expect(PreferencesStore.loadUserPreferences).toBeCalled();
  });
});
