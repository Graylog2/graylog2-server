/**
 * @jest-environment <rootDir>/test/integration-environment.js
 */
// @flow strict
import * as React from 'react';
import { cleanup, waitForElement, render, fireEvent } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import { StoreMock as MockStore } from 'helpers/mocking';
import history from 'util/History';
import Routes from 'routing/Routes';
import AppRouter from 'routing/AppRouter';

import viewsBindings from 'views/bindings';

jest.mock('views/stores/DashboardsStore', () => ({
  DashboardsActions: {
    search: jest.fn(() => Promise.resolve()),
  },
  DashboardsStore: MockStore(
    ['listen', () => jest.fn()],
    ['getInitialState', () => ({
      listen: [],
      pagination: {
        total: 100,
        page: 1,
        perPage: 20,
      },
    })],
  ),
}));
jest.mock('stores/users/CurrentUserStore', () => MockStore(
  ['listen', () => jest.fn()],
  'get',
  ['getInitialState', () => ({
    currentUser: {
      full_name: 'Betty Holberton',
      username: 'betty',
      permissions: ['dashboards:create'],
    },
  })],
));

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: jest.fn(() => global.api_url),
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2DevMode: jest.fn(() => false),
  isFeatureEnabled: jest.fn(() => true),
  isCloud: jest.fn(() => false),
}));
jest.mock('stores/sessions/SessionStore', () => ({
  isLoggedIn: jest.fn(() => true),
  getSessionId: jest.fn(() => 'foobar'),
}));
jest.mock('views/components/searchbar/QueryInput', () => () => <span>Query Editor</span>);

jest.unmock('logic/rest/FetchProvider');

describe('Create a new dashboard', () => {
  beforeAll(() => {
    PluginStore.register(new PluginManifest({}, viewsBindings));
  });

  beforeEach(() => {
    jest.setTimeout(30000);
  });

  afterEach(cleanup);

  it('using Dashboards Page', async () => {
    const { getByText, getAllByText } = render(<AppRouter />);
    history.push(Routes.DASHBOARDS);

    const button = await waitForElement(() => getAllByText('Create new dashboard')[0], { timeout: 15000 });
    fireEvent.click(button);
    await waitForElement(() => getByText(/This dashboard has no widgets yet/), { timeout: 15000 });
  });

  it('by going to the new dashboards endpoint', async () => {
    const { getByText } = render(<AppRouter />);

    history.push(Routes.pluginRoute('DASHBOARDS_NEW'));

    await waitForElement(() => getByText(/This dashboard has no widgets yet/), { timeout: 15000 });
  });
});
