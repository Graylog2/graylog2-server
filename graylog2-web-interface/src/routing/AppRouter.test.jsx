// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import mockComponent from 'helpers/mocking/MockComponent';
import { CombinedProviderMock as MockCombinedProvider, StoreMock as MockStore } from 'helpers/mocking';

import CurrentUserProvider from 'contexts/CurrentUserProvider';

import AppRouter from './AppRouter';

jest.mock('components/throughput/GlobalThroughput', () => mockComponent('GlobalThroughput'));

jest.mock('injection/CombinedProvider', () => {
  const mockCurrentUserStore = MockStore('get', 'listen', ['getInitialState', () => ({
    currentUser: {
      full_name: 'Ares Vallis',
      username: 'ares',
      permissions: ['*'],
    },
  })]);

  return new MockCombinedProvider({
    CurrentUser: { CurrentUserStore: mockCurrentUserStore },
    Notifications: { NotificationsActions: { list: jest.fn() }, NotificationsStore: MockStore() },
  });
});

// To prevent exceptions from getting swallowed
jest.mock('components/errors/RouterErrorBoundary', () => mockComponent('RouterErrorBoundary'));

jest.mock('pages/StartPage', () => () => <>This is the start page</>);

describe('AppRouter', () => {
  it('routes to Getting Started Page for `/` or empty location', async () => {
    const { findByText } = render(
      <CurrentUserProvider>
        <AppRouter />
      </CurrentUserProvider>,
    );

    await findByText('This is the start page');
  });
});
