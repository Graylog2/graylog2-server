// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import mockComponent from 'helpers/mocking/MockComponent';
import { CombinedProviderMock as MockCombinedProvider, StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import { admin } from 'fixtures/users';

import CurrentUserContext from 'contexts/CurrentUserContext';
import usePluginEntities from 'views/logic/usePluginEntities';
import history from 'util/History';

import AppRouter from './AppRouter';

jest.mock('components/throughput/GlobalThroughput', () => mockComponent('GlobalThroughput'));

jest.mock('injection/CombinedProvider', () => {
  return new MockCombinedProvider({
    Notifications: { NotificationsActions: { list: jest.fn() }, NotificationsStore: MockStore() },
  });
});

// To prevent exceptions from getting swallowed
jest.mock('components/errors/RouterErrorBoundary', () => mockComponent('RouterErrorBoundary'));

jest.mock('pages/StartPage', () => () => <>This is the start page</>);
jest.mock('views/logic/usePluginEntities');
jest.mock('components/layout/Footer', () => mockComponent('Footer'));

describe('AppRouter', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
  });

  const AppRouterWithContext = () => (
    <CurrentUserContext.Provider value={admin}>
      <AppRouter />
    </CurrentUserContext.Provider>
  );

  it('routes to Getting Started Page for `/` or empty location', async () => {
    const { findByText } = render(<AppRouterWithContext />);

    await findByText('This is the start page');
  });

  it('renders null-parent component plugin routes without application chrome', async () => {
    asMock(usePluginEntities).mockReturnValue([{ parentComponent: null, component: () => <span>Hey there!</span> }]);

    const { findByText, queryByTitle } = render(<AppRouterWithContext />);

    await findByText('Hey there!');

    expect(queryByTitle('Graylog Logo')).toBeNull();
  });

  it('renders a not found page for unknown URLs', async () => {
    const { findByText } = render(<AppRouterWithContext />);

    history.push('/this-url-is-not-registered-and-should-never-be');

    await findByText('Page not found');
  });
});
