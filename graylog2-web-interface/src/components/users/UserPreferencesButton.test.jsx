import React from 'react';
import { mount } from 'enzyme';

import { CombinedProviderMock, StoreMock } from 'helpers/mocking';

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

    // eslint-disable-next-line global-require
    const UserPreferencesButton = require('components/users/UserPreferencesButton');
    const userName = 'Full';
    const instance = mount(<UserPreferencesButton userName={userName} />);

    expect(instance).toMatchSnapshot();
    expect(instance.find('button')).toBeDefined();

    instance.find('button').simulate('click');

    expect(PreferencesStore.loadUserPreferences).toBeCalled();
  });
});
