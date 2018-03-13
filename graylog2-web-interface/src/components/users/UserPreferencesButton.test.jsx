import React from 'react';

import { CombinedProviderMock, StoreMock } from 'helpers/mocking';

const e = () => {
  // eslint-disable-next-line global-require
  const enzyme = require('enzyme');
  // eslint-disable-next-line global-require
  const Adapter = require('enzyme-adapter-react-15');
  enzyme.configure({ adapter: new Adapter() });
  return enzyme;
};

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
    const instance = e().mount(<UserPreferencesButton userName={userName} />);

    expect(instance).toMatchSnapshot();
    expect(instance.find('button')).toBeDefined();

    instance.find('button').simulate('click');

    expect(PreferencesStore.loadUserPreferences).toBeCalled();
  });
});
