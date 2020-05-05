// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import { CombinedProviderMock as MockCombinedProvider, StoreMock as MockStore } from 'helpers/mocking';
import AppRouter from './AppRouter';

jest.mock('pages', () => ({
  StartPage: mockComponent('StartPage'),
}));
jest.mock('components/throughput/GlobalThroughput', () => mockComponent('GlobalThroughput'));

jest.mock('injection/CombinedProvider', () => {
  const mockCurrentUserStoretore = MockStore('get', 'listen', ['getInitialState', () => ({
    currentUser: {
      full_name: 'Ares Vallis',
      username: 'ares',
      permissions: ['*'],
    },
  })]);
  return new MockCombinedProvider({
    CurrentUser: { CurrentUserStore: mockCurrentUserStoretore },
    Notifications: { NotificationsActions: { list: jest.fn() }, NotificationsStore: MockStore() },
  });
});

// To prevent exceptions from getting swallwoed
jest.mock('components/errors/RouterErrorBoundary', () => mockComponent('RouterErrorBoundary'));

describe('AppRouter', () => {
  it('routes to Getting Started Page for `/` or empty location', () => {
    const wrapper = mount(<AppRouter />);
    expect(wrapper.find('StartPage')).toExist();
  });
});
