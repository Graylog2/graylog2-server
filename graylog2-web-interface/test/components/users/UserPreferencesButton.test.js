import React from 'react';

describe('UserPreferencesButton', function () {
  beforeEach(() => {
    jest.resetModules();
  });

  it('should load user data when user clicks edit button', function () {
    const mockLoadUserPreferences = jest.fn();

    jest.doMock('injection/CombinedProvider', () => {
      return {
        get: (name) => {
          if (name === 'Preferences') {
            return {
              PreferencesStore: {
                get: () => jest.fn(() => ({})),
                listen: jest.fn(),
                loadUserPreferences: mockLoadUserPreferences,
              },
              PreferencesActions: {},
            };
          }
          const result = {};
          result[`${name}Store`] = {
            get: () => jest.fn(() => ({})),
            listen: jest.fn(),
          };
          result[`${name}Actions`] = {};
          return result;
        },
      };
    });


    const UserPreferencesButton = require('components/users/UserPreferencesButton');
    const userName = 'Full';
    const instance = require('enzyme').mount(<UserPreferencesButton userName={userName} />);

    expect(instance).toMatchSnapshot();
    expect(instance.find('button')).toBeDefined();

    instance.find('button').simulate('click');

    expect(mockLoadUserPreferences).toBeCalled();
  });
});
